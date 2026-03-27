package com.mordan.aihub.lowcode.workflow.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mordan.aihub.lowcode.ai.GenerateCodeAiService;
import com.mordan.aihub.lowcode.workflow.state.GeneratedCode;
import com.mordan.aihub.lowcode.workflow.state.GenerationWorkflowContext;
import com.mordan.aihub.lowcode.workflow.state.WorkflowState;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 代码生成节点
 * 根据解析后的需求，调用 AI 服务生成完整的 Vue 3 前端项目代码。
 * 职责：
 *   1. 组装结构化 userPrompt（XML 标签包裹各上下文段落）
 *   2. 调用 AI 服务获取 JSON 字符串
 *   3. 解析 JSON → GeneratedCode；解析失败时尝试自动修复
 *   4. 更新 WorkflowContext，供后续节点使用
 */
@Slf4j
@Component
public class GenerateCodeNode implements NodeAction<WorkflowState> {

    @Resource
    private GenerateCodeAiService generateCodeAiService;

    @Resource
    private ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────────
    // 主入口
    // ─────────────────────────────────────────────────────────────

    @Override
    public Map<String, Object> apply(WorkflowState state) {
        GenerationWorkflowContext ctx = state.context();

        // 1. 构建结构化 userPrompt
        String userPrompt = buildUserPrompt(ctx);
        log.debug("Generated userPrompt (length={}):\n{}", userPrompt.length(), userPrompt);

        // 2. 调用 AI 服务生成代码
        GeneratedCode generatedCode = callAiAndParse(ctx, userPrompt);

        // 3. 更新上下文
        ctx.setGeneratedCode(generatedCode);

        // 只有本次生成成功时才重置重试计数；失败时保留计数供重试节点判断
        if (ctx.getSuccess()) {
            ctx.setRetryCount(0);
        }

        return WorkflowState.saveContext(ctx);
    }

    // ─────────────────────────────────────────────────────────────
    // 构建 userPrompt
    // ─────────────────────────────────────────────────────────────

    /**
     * 将上下文各字段拼装为结构化 userPrompt。
     * 使用 XML 标签包裹每个段落，帮助模型准确定位各段内容。
     */
    private String buildUserPrompt(GenerationWorkflowContext ctx) {
        StringBuilder sb = new StringBuilder();

        // ── 需求结构（必填）──
        appendSection(sb, "需求结构", ctx.getParsedIntent());

        // ── API 文档（可选）──
        if (hasText(ctx.getApiDocText())) {
            appendSection(sb, "API文档", ctx.getApiDocText());
        } else {
            appendSection(sb, "API文档", "无，所有数据使用前端 mock，禁止发起任何网络请求");
        }

        // ── 图片需求关键词（可选）──
//        if (hasText(ctx.get())) {
//            appendSection(sb, "图片需求关键词", ctx.getImageNeeds());
//        }

        // ── 已有代码快照 + 迭代说明（可选）──
        if (hasText(ctx.getParentCodeSnapshot())) {
            appendSection(sb, "已有代码快照", ctx.getParentCodeSnapshot());
            appendSection(sb, "迭代说明",
                    "这是一次迭代修改任务。请基于【已有代码快照】进行最小化改动：\n" +
                            "- 只修改需求中明确要求变更的部分\n" +
                            "- 输出的 files 数组中只包含被修改的文件，未改动文件不输出\n" +
                            "- 每个修改文件在顶部注释标注改动原因：// [迭代] 原因描述\n" +
                            "- 禁止重构未涉及需求的代码结构");
        } else {
            appendSection(sb, "已有代码快照", "无");
            appendSection(sb, "迭代说明", "这是一次全新生成任务，请输出所有必须文件。");
        }

        return sb.toString();
    }

    /**
     * 向 StringBuilder 追加一个 XML 标签包裹的段落。
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

    /**
     * 调用 AI 服务并将返回的 JSON 字符串解析为 GeneratedCode。
     * 解析失败时执行一次 JSON 片段提取修复；修复仍失败则标记上下文失败。
     */
    private GeneratedCode callAiAndParse(GenerationWorkflowContext ctx, String userPrompt) {
        String rawResult;
        try {
            rawResult = generateCodeAiService.generateCode(userPrompt);
            if (!hasText(rawResult)) {
                log.error("AI service returned empty response");
                return failWith(ctx, "代码生成失败：AI 返回内容为空");
            }
            rawResult = rawResult.trim();
        } catch (Exception e) {
            log.error("AI service call failed", e);
            return failWith(ctx, "代码生成失败：" + e.getMessage());
        }

        // 第一次尝试：直接解析
        try {
            GeneratedCode result = objectMapper.readValue(rawResult, GeneratedCode.class);
            log.info("Code generation and JSON parsing succeeded");
            return result;
        } catch (Exception firstEx) {
            log.warn("First JSON parse failed, attempting extraction recovery. Error: {}", firstEx.getMessage());
        }

        // 第二次尝试：从响应文本中提取 JSON 片段（模型有时会在 JSON 前后添加说明文字）
        String extracted = extractJsonFragment(rawResult);
        if (extracted != null) {
            try {
                GeneratedCode result = objectMapper.readValue(extracted, GeneratedCode.class);
                log.info("JSON extraction recovery succeeded");
                return result;
            } catch (Exception secondEx) {
                log.error("JSON extraction recovery also failed. Extracted fragment:\n{}", extracted, secondEx);
            }
        } else {
            log.error("Could not locate JSON fragment in AI response. Raw response (first 500 chars):\n{}",
                    rawResult.substring(0, Math.min(rawResult.length(), 500)));
        }

        return failWith(ctx, "代码生成格式解析失败，请重试");
    }

    /**
     * 从原始文本中尝试提取 {"files":[...]} 片段。
     * 处理模型在 JSON 前后输出多余文字或 markdown 代码块的情况。
     */
    private String extractJsonFragment(String text) {
        // 去除可能的 markdown 代码块标记
        String cleaned = text
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();

        // 定位 {"files": 起始位置
        int start = cleaned.indexOf("{\"files\"");
        if (start < 0) {
            // 兼容空格写法：{ "files"
            start = cleaned.indexOf("{ \"files\"");
        }
        if (start < 0) {
            return null;
        }

        // 从 start 向后匹配大括号，找到完整 JSON 对象的结束位置
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = start; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (c == '\\' && inString) {
                escape = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (!inString) {
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        return cleaned.substring(start, i + 1);
                    }
                }
            }
        }

        // 括号不匹配时退回到最后一个 } 的简单截取
        int end = cleaned.lastIndexOf('}');
        if (end > start) {
            return cleaned.substring(start, end + 1);
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────
    // 工具方法
    // ─────────────────────────────────────────────────────────────

    /**
     * 标记上下文为失败状态，并返回空 GeneratedCode。
     */
    private GeneratedCode failWith(GenerationWorkflowContext ctx, String reason) {
        ctx.setSuccess(false);
        ctx.setFailureReason(reason);
        return GeneratedCode.builder().build();
    }

    /**
     * 判断字符串是否有实际内容（非 null、非空白）。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}