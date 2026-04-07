package com.mordan.aihub.lowcode.workflow.node;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mordan.aihub.lowcode.ai.LowCodeGenerateAiService;
import com.mordan.aihub.lowcode.infrastructure.sse.SseEmitterRegistry;
import com.mordan.aihub.lowcode.workflow.state.GenerationWorkflowContext;
import com.mordan.aihub.lowcode.workflow.state.ParsedIntent;
import com.mordan.aihub.lowcode.workflow.state.QualityResult;
import com.mordan.aihub.lowcode.workflow.state.WorkflowState;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * 代码生成节点
 *
 * 职责：
 *   1. 组装结构化 userPrompt（XML 标签包裹各上下文段落）
 *   2. 将 ValidateCodeNode 产出的 LLM 建议注入 prompt（质检反馈闭环）
 *   3. 调用 AI 生成代码，解析 JSON → GeneratedCode
 *   4. 解析失败时自动提取 JSON 片段恢复
 *
 * LLM 质检反馈闭环说明：
 *   ValidateCodeNode 的二级 LLM 校验结果（llmSuggestions）不触发修复，
 *   而是通过本节点在下一轮生成时注入 prompt，让模型主动规避上次的质量问题。
 *   这比"发现问题→触发修复"更稳定，避免了修复节点因 LLM 误判导致的死循环。
 */
@Slf4j
@Component
public class GenerateCodeNode implements NodeAction<WorkflowState> {

    @Resource
    private LowCodeGenerateAiService lowCodeGenerateAiService;
    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private SseEmitterRegistry sseEmitterRegistry;

    // ─────────────────────────────────────────────────────────────
    // 主入口
    // ─────────────────────────────────────────────────────────────

    @Override
    public Map<String, Object> apply(WorkflowState state) {
        GenerationWorkflowContext ctx = state.context();
        int retryCount = ctx.getRetryCount() == null ? 0 : ctx.getRetryCount();


        // 1. 构建结构化 userPrompt
        String userPrompt;
        try {
            userPrompt = buildUserPrompt(ctx);
        } catch (Exception e) {
            log.error("Failed to build user prompt", e);
            ctx.setSuccess(false);
            ctx.setFailureReason("构建提示词失败：" + e.getMessage());
            return WorkflowState.saveContext(ctx);
        }
        log.debug("Generated userPrompt (length={}):\n{}", userPrompt.length(), userPrompt);

        sseEmitterRegistry.sendProgress(ctx.getTaskId(), "开始代码生成", null, retryCount);

        // 2. 调用 AI 生成代码
        String generatedResult = callAiAndParse(ctx, userPrompt);

        // 3. 更新上下文
        ctx.setGeneratedResult(generatedResult);

        if (Objects.nonNull(generatedResult)) {
            sseEmitterRegistry.sendProgress(ctx.getTaskId(), "代码生成成功", null, retryCount);
        } else {
            sseEmitterRegistry.sendProgress(ctx.getTaskId(), "代码生成失败", ctx.getFailureReason(),  retryCount);
        }

        return WorkflowState.saveContext(ctx);
    }

    // ─────────────────────────────────────────────────────────────
    // 构建 userPrompt
    // ─────────────────────────────────────────────────────────────

    /**
     * 将上下文各字段拼装为结构化 userPrompt。
     * 使用 XML 标签包裹，帮助模型准确定位各段内容。
     */
    private String buildUserPrompt(GenerationWorkflowContext ctx) throws JsonProcessingException {
        ParsedIntent parsedIntent = ctx.getParsedIntent();

        StringBuilder sb = new StringBuilder();

        // ── 需求结构（必填）──
        String parsedIntentJson = parsedIntent != null
                ? objectMapper.writeValueAsString(parsedIntent)
                : ctx.getParsedIntentJson();
        appendSection(sb, "需求描述", parsedIntentJson);

        // ── API 文档（可选）──
        if (hasText(ctx.getApiDocText())) {
            appendSection(sb, "API文档", ctx.getApiDocText());
        } else {
            appendSection(sb, "API文档", "无，所有数据使用前端 mock，禁止发起任何网络请求");
        }

        // ── 图片需求关键词（可选）──
//        if (hasText(ctx.getImageNeeds())) {
//            appendSection(sb, "图片需求关键词", ctx.getImageNeeds());
//        }

//        // ── 迭代模式：基于已有项目（每个 app 仅保留一份最新代码）──
//        // appId 不为空表示已有项目，这是一次迭代修改
//        if (hasText(ctx.getAppId()) && hasText(ctx.getExistingProjectSummary())) {
//            appendSection(sb, "迭代说明",
//                    "这是一次迭代修改任务，你已经注册了文件操作工具，请按以下步骤工作：\n" +
//                    "1. 根据用户需求和已有项目文件摘要，判断哪些文件需要修改\n" +
//                    "2. **所有文件操作路径都是相对于项目根目录的相对路径**\n" +
//                    "3. 使用 readDir 工具读取项目目录确认结构，参数传入相对路径（根目录传空字符串或\".\"）\n" +
//                    "4. 使用 readFile 工具读取**只需要修改**的文件内容（禁止读取无关文件，节省上下文空间）\n" +
//                    "5. 分析代码后使用对应工具完成修改：\n" +
//                    "   - 修改现有文件 → modifyFile（传入相对路径）\n" +
//                    "   - 创建新文件 → writeFile（传入相对路径）\n" +
//                    "   - 删除文件 → deleteFile（传入相对路径）\n" +
//                    "6. 所有修改完成后，使用 exit 工具，然后输出最终 JSON 结果\n" +
//                    "   - JSON **只需要包含 summary 字段**（修改摘要）\n" +
//                    "   - **必须删除 files 字段**，不输出 files，因为所有修改已经通过工具写入磁盘");
//            // 注入已有项目摘要，帮助模型理解现有文件结构
//            appendSection(sb, "已有项目文件摘要", ctx.getExistingProjectSummary());
//        } else {
//            // 全新项目
//            appendSection(sb, "迭代说明",
//                    "这是一次全新项目生成任务，你已经注册了文件操作工具，请按以下步骤工作：\n" +
//                    "1. 根据需求，创建所有必须的项目文件\n" +
//                    "2. **所有文件操作路径都是相对于项目根目录的相对路径**\n" +
//                    "3. 使用 writeFile 工具逐个创建所有需要的文件\n" +
//                    "4. 所有文件创建完成后，使用 exit 工具，然后输出最终 JSON 结果\n" +
//                    "   - JSON **只需要包含 summary 字段**（项目摘要）\n" +
//                    "   - **必须删除 files 字段**，不输出 files，因为所有文件已经通过工具写入磁盘");
//        }

        // ── LLM 质检反馈（上轮 ValidateCodeNode 二级校验的建议，可选）──
        // 这是质检反馈闭环的关键：上轮发现的质量问题注入本轮生成，主动规避
        if (Objects.nonNull(ctx.getQualityResult())) {
            appendSection(sb, "", this.buildErrorFixPrompt(ctx.getQualityResult()));
        }

        return sb.toString();
    }

    /**
     * 构造错误修复提示词
     */
    private String buildErrorFixPrompt(QualityResult qualityResult) {
        StringBuilder errorInfo = new StringBuilder();
        errorInfo.append("\n\n## 上次生成的代码存在以下问题，请修复：\n");
        // 添加错误列表
        qualityResult.getErrors().forEach(error ->
                errorInfo.append("- ").append(error).append("\n"));
        // 添加修复建议（如果有）
        if (qualityResult.getSuggestions() != null && !qualityResult.getSuggestions().isEmpty()) {
            errorInfo.append("\n## 修复建议：\n");
            qualityResult.getSuggestions().forEach(suggestion ->
                    errorInfo.append("- ").append(suggestion).append("\n"));
        }
        errorInfo.append("\n请根据上述问题和建议重新生成代码，确保修复所有提到的问题。");
        return errorInfo.toString();
    }

    /**
     * 追加一个 XML 标签包裹的段落。
     * 格式：<tagName>\n{content}\n</tagName>\n\n
     */
    private void appendSection(StringBuilder sb, String tagName, String content) {
        sb.append("<").append(tagName).append(">\n")
          .append(content == null ? "" : content.trim())
          .append("\n</").append(tagName).append(">\n\n");
    }

    // ─────────────────────────────────────────────────────────────
    // AI 调用 & JSON 解析
    // ─────────────────────────────────────────────────────────────

    private String callAiAndParse(GenerationWorkflowContext ctx, String userPrompt) {
        ctx.setCodeSuccess(true);
        String generatedResult;
        try {
            generatedResult = lowCodeGenerateAiService.generateCode(ctx.getAppId(), userPrompt);
            if (!hasText(generatedResult)) {
                failWith(ctx, "代码生成失败：AI 返回内容为空");
                return null;
            }
            generatedResult = generatedResult.trim();
        } catch (Exception e) {
            log.error("AI service call failed", e);
            failWith(ctx, "代码生成失败：" + e.getMessage());
            return null;
        }

        return generatedResult;
    }

    // ─────────────────────────────────────────────────────────────
    // 工具方法
    // ─────────────────────────────────────────────────────────────

    private void failWith(GenerationWorkflowContext ctx, String reason) {
        ctx.setCodeSuccess(false);
        ctx.setFailureReason(reason);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
