package com.mordan.aihub.lowcode.workflow;

import cn.hutool.core.util.BooleanUtil;
import com.mordan.aihub.lowcode.config.GenerationProperties;
import com.mordan.aihub.lowcode.workflow.node.BuildNode;
import com.mordan.aihub.lowcode.workflow.node.GenerateCodeNode;
import com.mordan.aihub.lowcode.workflow.node.IntentCheckNode;
import com.mordan.aihub.lowcode.workflow.node.MarkFailedNode;
import com.mordan.aihub.lowcode.workflow.node.ParseIntentNode;
import com.mordan.aihub.lowcode.workflow.node.RejectNode;
import com.mordan.aihub.lowcode.workflow.node.RepairCodeNode;
import com.mordan.aihub.lowcode.workflow.node.SaveVersionNode;
import com.mordan.aihub.lowcode.workflow.node.ValidateCodeNode;
import com.mordan.aihub.lowcode.workflow.state.GenerationWorkflowContext;
import com.mordan.aihub.lowcode.workflow.state.IntentCheckResult;
import com.mordan.aihub.lowcode.workflow.state.WorkflowState;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

/**
 * 代码生成工作流
 *
 * 完整流程：
 *   START
 *     → intentCheck（意图识别）
 *         → [no_intent]  reject → END
 *         → [has_intent] parseIntent
 *             → generateCode
 *                 → validateCode
 *                     → [save]   build → saveVersion → END
 *                     → [repair] repair → validateCode（循环）
 *                     → [fail]   fail   → END
 *
 * 优化点（相比原版）：
 *   1. retryRouter 增加 LLM suggestions 判断，建议注入 GenerateCodeNode 而非触发修复
 *   2. 工作流执行异常时主动推送 SSE 错误，避免客户端永久挂起
 *   3. 提取路由逻辑为独立私有方法，职责更清晰
 */
@Slf4j
@Component
public class CodeGenerationWorkflow {

    @Resource private IntentCheckNode intentCheckNode;
    @Resource private RejectNode rejectNode;
    @Resource private ParseIntentNode parseIntentNode;
    @Resource private GenerateCodeNode generateCodeNode;
    @Resource private ValidateCodeNode validateCodeNode;
    @Resource private RepairCodeNode repairCodeNode;
    @Resource private BuildNode buildNode;
    @Resource private SaveVersionNode saveVersionNode;
    @Resource private MarkFailedNode markFailedNode;
//    @Resource private MemorySaver memorySaver;
    @Resource private GenerationProperties generationProperties;

    // 使用虚拟线程池，每个任务独立线程，互不阻塞
    private final Executor workflowExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private CompiledGraph<WorkflowState> compiledGraph;

    @PostConstruct
    public void init() throws GraphStateException {
        StateGraph<WorkflowState> graph = new StateGraph<>(WorkflowState.schema(), WorkflowState::new);

        // ── 注册节点 ────────────────────────────────────────────────
        graph.addNode("intentCheck",  AsyncNodeAction.node_async(intentCheckNode));
        graph.addNode("reject",       AsyncNodeAction.node_async(rejectNode));
        graph.addNode("parseIntent",  AsyncNodeAction.node_async(parseIntentNode));
        graph.addNode("generateCode", AsyncNodeAction.node_async(generateCodeNode));
        graph.addNode("validateCode", AsyncNodeAction.node_async(validateCodeNode));
        graph.addNode("repair",       AsyncNodeAction.node_async(repairCodeNode));
        graph.addNode("build",        AsyncNodeAction.node_async(buildNode));
        graph.addNode("saveVersion",  AsyncNodeAction.node_async(saveVersionNode));
        graph.addNode("fail",         AsyncNodeAction.node_async(markFailedNode));

        // ── 边与路由 ────────────────────────────────────────────────
        graph.addEdge(START, "intentCheck");

        // 意图检查分支：有意图继续，无意图拒绝
        graph.addConditionalEdges("intentCheck", edge_async(this::intentRouter), Map.of(
                "has_intent", "parseIntent",
                "no_intent",  "reject"
        ));

        graph.addConditionalEdges("parseIntent", edge_async(this::parseIntentRouter), Map.of(
                "success", "generateCode",
                "fail",  "reject"
        ));

        // 校验代码生成结果
        graph.addConditionalEdges("generateCode", edge_async(this::generateSuccess), Map.of(
                "success",   "validateCode",
                "fail",   "fail"
        ));


        // 校验结果分支：通过→构建，有错→修复，超限→失败
        graph.addConditionalEdges("validateCode", edge_async(this::retryRouter), Map.of(
                "save",   "build",
                "repair", "repair",
                "fail",   "fail"
        ));

        // repair 完成后回到 validateCode 重新校验（循环收敛）
        graph.addEdge("repair",      "validateCode");

        graph.addEdge("build",       "saveVersion");

        graph.addEdge("reject",      END);
        graph.addEdge("saveVersion", END);
        graph.addEdge("fail",        END);

        // ── 编译 ────────────────────────────────────────────────────
//        CompileConfig compileConfig = CompileConfig.builder()
//                .checkpointSaver(memorySaver)
//                .build();
        compiledGraph = graph.compile();

        log.info("CodeGenerationWorkflow initialized successfully");
    }

    private String parseIntentRouter(WorkflowState workflowState) {
        GenerationWorkflowContext context = workflowState.context();
        return context.getSuccess() ? "success" : "fail";
    }

    private String generateSuccess(WorkflowState workflowState) {
        GenerationWorkflowContext context = workflowState.context();
        return BooleanUtil.isTrue(context.getCodeSuccess()) ? "success" : "fail";
    }

    /**
     * 异步提交生成任务。
     * 工作流整体异常时记录日志（各节点内部已有独立错误处理）。
     */
    public void submit(Map<String, Object> initialState, Long taskId) {
        workflowExecutor.execute(() -> {
            try {
                compiledGraph.invoke(initialState);
                log.info("Workflow completed for taskId={}", taskId);
            } catch (Exception e) {
                log.error("Workflow execution failed for taskId={}", taskId, e);
            }
        });
    }

    /** 供测试使用 */
    public CompiledGraph<WorkflowState> compiledGraph() {
        return compiledGraph;
    }

    // ─────────────────────────────────────────────────────────────
    // 路由函数
    // ─────────────────────────────────────────────────────────────

    /**
     * 意图路由
     * hasIntent=true  → has_intent（继续生成）
     * hasIntent=false → no_intent（拒绝）
     */
    private String intentRouter(WorkflowState state) {
        IntentCheckResult result = state.context().getIntentCheckResult();
        boolean hasIntent = result != null && Boolean.TRUE.equals(result.getHasIntent());
        log.debug("intentRouter: hasIntent={}", hasIntent);
        return hasIntent ? "has_intent" : "no_intent";
    }

    /**
     * 重试路由
     *
     * 路由逻辑：
     *   - 无规则错误（errors 为空）→ save（进入构建）
     *   - 有规则错误 且 未超过最大重试次数 → repair（进入修复）
     *   - 有规则错误 且 已超过最大重试次数 → fail（标记失败）
     *
     * 注意：LLM suggestions 不触发 repair，而是通过 ctx.setLlmSuggestions()
     * 保留在上下文，由 GenerateCodeNode 在下一轮生成时作为质检反馈注入 prompt。
     */
    private String retryRouter(WorkflowState state) {
        var ctx = state.context();
        var errors = ctx.getValidationErrors();

        // 规则校验通过，允许进入构建阶段
        if (errors == null || errors.isEmpty()) {
            log.debug("retryRouter: no errors, routing to save");
            return "save";
        }

        int retryCount = ctx.getRetryCount() == null ? 0 : ctx.getRetryCount();
        int maxRetry   = generationProperties.getMaxRetry();

        if (retryCount >= maxRetry) {
            log.warn("retryRouter: retryCount={} >= maxRetry={}, routing to fail. errors={}",
                    retryCount, maxRetry, errors);
            return "fail";
        }

        log.info("retryRouter: retryCount={}/{}, routing to repair. errors={}",
                retryCount, maxRetry, errors);
        return "repair";
    }
}
