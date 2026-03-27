package com.mordan.aihub.lowcode.workflow;

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
import com.mordan.aihub.lowcode.workflow.state.IntentCheckResult;
import com.mordan.aihub.lowcode.workflow.state.WorkflowState;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

/**
 * 代码生成工作流组装
 * 使用LangGraph4j构建有状态的代码生成流水线
 */
@Slf4j
@Component
public class CodeGenerationWorkflow {

    @Resource
    private IntentCheckNode intentCheckNode;
    @Resource
    private RejectNode rejectNode;
    @Resource
    private ParseIntentNode parseIntentNode;
    @Resource
    private GenerateCodeNode generateCodeNode;
    @Resource
    private ValidateCodeNode validateCodeNode;
    @Resource
    private RepairCodeNode repairCodeNode;
    @Resource
    private BuildNode buildNode;
    @Resource
    private SaveVersionNode saveVersionNode;
    @Resource
    private MarkFailedNode markFailedNode;
    @Resource
    private MemorySaver memorySaver;
    @Resource
    private GenerationProperties generationProperties;

    private final Executor workflowExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private CompiledGraph<WorkflowState> compiledGraph;

    @PostConstruct
    public void init() throws GraphStateException {
        StateGraph<WorkflowState> graph = new StateGraph<>(WorkflowState.schema(), WorkflowState::new);

        // 添加所有节点 - 使用 node_async 包装
        graph.addNode("intentCheck", AsyncNodeAction.node_async(intentCheckNode));
        graph.addNode("reject", AsyncNodeAction.node_async(rejectNode));
        graph.addNode("parseIntent", AsyncNodeAction.node_async(parseIntentNode));
        graph.addNode("generateCode", AsyncNodeAction.node_async(generateCodeNode));
        graph.addNode("validateCode", AsyncNodeAction.node_async(validateCodeNode));
        graph.addNode("repair", AsyncNodeAction.node_async(repairCodeNode));
        graph.addNode("build", AsyncNodeAction.node_async(buildNode));
        graph.addNode("saveVersion", AsyncNodeAction.node_async(saveVersionNode));
        graph.addNode("fail", AsyncNodeAction.node_async(markFailedNode));

        // 设置入口
        graph.addEdge(START, "intentCheck");

        // 意图判断分支
        graph.addConditionalEdges("intentCheck", edge_async(this::intentRouter), Map.of(
                "has_intent", "parseIntent",
                "no_intent", "reject"
        ));

        // parseIntent -> generateCode
        graph.addEdge("parseIntent", "generateCode");

        // generateCode -> validateCode
        graph.addEdge("generateCode", "validateCode");

        // validateCode 分支判断
        graph.addConditionalEdges("validateCode", edge_async(this::retryRouter), Map.of(
                "save", "build",
                "repair", "repair",
                "fail", "fail"
        ));

        // repair -> validateCode（循环回去重新校验）
        graph.addEdge("repair", "validateCode");

        // build -> saveVersion（构建完成后保存）
        graph.addEdge("build", "saveVersion");

        // 设置结束点
        graph.addEdge("reject", END);
        graph.addEdge("saveVersion", END);
        graph.addEdge("fail", END);

        // 编译
        CompileConfig compileConfig = CompileConfig.builder()
                .checkpointSaver(memorySaver)
                .build();
        compiledGraph = graph.compile(compileConfig);

        log.info("CodeGenerationWorkflow initialized successfully");
    }

    /**
     * 提交一个新的生成任务（异步执行）
     */
    public void submit(Map<String, Object> initialState, Long taskId) {
        workflowExecutor.execute(() -> {
            try {
                compiledGraph.invoke(initialState);
                log.info("Workflow completed for task: {}", taskId);
            } catch (Exception e) {
                log.error("Workflow execution failed for task: {}", taskId, e);
            }
        });
    }

    /**
     * 意图路由：判断是否有生成网站意图
     */
    private String intentRouter(WorkflowState state) {
        IntentCheckResult intentResult = state.context().getIntentCheckResult();
        boolean hasIntent = intentResult != null && intentResult.getHasIntent();
        return hasIntent ? "has_intent" : "no_intent";
    }

    /**
     * 重试路由：根据校验结果和重试次数判断下一步
     */
    private String retryRouter(WorkflowState state) {
        var errors = state.context().getValidationErrors();
        if (errors == null || errors.isEmpty()) {
            return "save";
        }
        Integer retryCount = state.context().getRetryCount();
        int maxRetry = generationProperties.getMaxRetry();
        if (retryCount != null && retryCount >= maxRetry) {
            return "fail";
        }
        return "repair";
    }

    /**
     * 获取编译好的工作流（供测试用）
     */
    public CompiledGraph<WorkflowState> compiledGraph() {
        return compiledGraph;
    }

}
