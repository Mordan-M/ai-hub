package com.mordan.aihub.lowcode.workflow.node;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.mordan.aihub.lowcode.ai.LowCodeGenerateAiService;
import com.mordan.aihub.lowcode.workflow.state.GeneratedCode;
import com.mordan.aihub.lowcode.workflow.state.GenerationWorkflowContext;
import com.mordan.aihub.lowcode.workflow.state.ParsedIntent;
import com.mordan.aihub.lowcode.workflow.state.WorkflowState;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

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

    // ─────────────────────────────────────────────────────────────
    // 主入口
    // ─────────────────────────────────────────────────────────────

    @Override
    public Map<String, Object> apply(WorkflowState state) {
        GenerationWorkflowContext ctx = state.context();

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


        // 2. 调用 AI 生成代码
        GeneratedCode generatedCode = callAiAndParse(ctx, userPrompt);

        // 3. 更新上下文
        ctx.setGeneratedCode(generatedCode);

        // 生成成功时重置重试计数；失败时保留供路由判断
        if (Boolean.TRUE.equals(ctx.getCodeSuccess())) {
            ctx.setRetryCount(0);
            // 成功生成后清除上轮 LLM 建议，避免重复注入
            ctx.setLlmSuggestions(null);
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
        appendSection(sb, "需求结构", parsedIntentJson);

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

        // ── 迭代模式：基于已有项目（每个 app 仅保留一份最新代码）──
        // appId 不为空表示已有项目，这是一次迭代修改
        if (hasText(ctx.getAppId())) {
            appendSection(sb, "迭代说明",
                    "这是一次迭代修改任务。这是一个已有项目，请基于用户新需求进行最小化改动：\n" +
                    "- 只修改需求中明确要求变更的部分\n" +
                    "- 输出的 files 数组中只包含被修改的文件，未改动文件不输出\n" +
                    "- 每个修改文件在顶部注释标注改动原因：// [迭代] 原因描述\n" +
                    "- 禁止重构未涉及需求的代码结构");
            // 注入已有项目摘要，帮助模型理解现有文件结构
            if (hasText(ctx.getExistingProjectSummary())) {
                appendSection(sb, "已有项目文件摘要", ctx.getExistingProjectSummary());
            }
        } else {
            appendSection(sb, "迭代说明", "这是一次全新生成任务，请输出所有必须文件。");
        }

        // ── LLM 质检反馈（上轮 ValidateCodeNode 二级校验的建议，可选）──
        // 这是质检反馈闭环的关键：上轮发现的质量问题注入本轮生成，主动规避
        if (hasText(ctx.getLlmSuggestions())) {
            appendSection(sb, "上轮质检建议",
                    "上一次生成存在以下质量问题，本次生成请特别注意避免：\n" +
                    ctx.getLlmSuggestions());
        }

        return sb.toString();
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

    private GeneratedCode callAiAndParse(GenerationWorkflowContext ctx, String userPrompt) {
        ctx.setCodeSuccess(true);
        String rawResult;
        try {
            rawResult = lowCodeGenerateAiService.generateCode(ctx.getAppId(), userPrompt);
            if (!hasText(rawResult)) {
                return failWith(ctx, "代码生成失败：AI 返回内容为空");
            }
            rawResult = rawResult.trim();
        } catch (Exception e) {
            log.error("AI service call failed", e);
            return failWith(ctx, "代码生成失败：" + e.getMessage());
        }

        // 第一次：直接解析
        try {
            GeneratedCode result = objectMapper.readValue(rawResult, GeneratedCode.class);
            log.info("JSON parse succeeded on first attempt");
            return result;
        } catch (Exception e) {
            log.warn("First parse failed: {}", e.getMessage());
        }

        // 第二次：剥离 markdown 代码块 + 提取 JSON 片段
        String extracted = extractJsonFragment(rawResult);
        if (extracted != null) {
            try {
                GeneratedCode result = objectMapper.readValue(extracted, GeneratedCode.class);
                log.info("JSON parse succeeded after fragment extraction");
                return result;
            } catch (Exception e) {
                log.warn("Second parse failed: {}", e.getMessage());
                // 继续尝试第三次，把提取出的片段作为修复对象
                rawResult = extracted;
            }
        }

        // 第三次：启发式修复后重试
        String repaired = repairJsonString(rawResult);
        if (repaired != null) {
            try {
                GeneratedCode result = objectMapper.readValue(repaired, GeneratedCode.class);
                log.info("JSON parse succeeded after heuristic repair");
                return result;
            } catch (Exception e) {
                logParseErrorContext(rawResult, e);
                log.error("Third parse failed after repair: {}", e.getMessage());
            }
        }

        log.error("All JSON parse attempts failed. raw head={}",
                rawResult.substring(0, Math.min(rawResult.length(), 300)));
        return failWith(ctx, "代码生成格式解析失败，请重试");
    }

    /**
     * 定位 JSON 解析失败的大致位置，输出上下文片段便于调试
     */
    private void logParseErrorContext(String raw, Exception e) {
        String msg = e.getMessage();
        // 从错误信息提取列号
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("column: (\\d+)")
                .matcher(msg);
        if (m.find()) {
            int col = Integer.parseInt(m.group(1));
            int start = Math.max(0, col - 100);
            int end   = Math.min(raw.length(), col + 100);
            log.error("Parse error near column {}, context:\n...{}...",
                    col, raw.substring(start, end));
        }
    }

    /**
     * 启发式 JSON 修复：处理模型最常见的几种格式错误。
     *
     * 修复策略：
     * 1. 去除 content 字段值中的真实换行符（替换为 \n）
     * 2. 修复 content 值中未转义的双引号（简单启发式，非完美）
     * 3. 去除 JSON 结尾的多余逗号（trailing comma）
     */
    private String repairJsonString(String raw) {
        if (raw == null) return null;

        // 预处理：修复 content 字段值中常见的反斜杠问题
        String preprocessed = preprocessContentFields(raw);

        try {
            ObjectMapper lenientMapper = JsonMapper.builder()
                    .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
                    .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
                    .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
                    .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                    .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER) // 关键：允许任意字符的反斜杠转义
                    .build();

            Object parsed = lenientMapper.readValue(preprocessed, Object.class);
            return objectMapper.writeValueAsString(parsed);
        } catch (Exception e) {
            log.warn("Heuristic JSON repair failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 预处理：修复模型输出中常见的反斜杠问题。
     * 核心问题：模型有时把 { "key": "val" } 输出为 {\"key\": \"val\"}（多加了反斜杠）
     * 或者把反斜杠本身漏转义，如 \d 应该是 \\d
     */
    private String preprocessContentFields(String raw) {
        // ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER 已经能处理大多数情况
        // 这里只处理一种特殊情况：\" 出现在 JSON key 位置（不在字符串值内）
        // 表现为: {"files":[{\"path\":\"xxx\"}]} 整个都被多加了反斜杠
        if (raw.contains("{\\\"")) {
            // 整体被多加了一层反斜杠转义，先 unescape 一次
            return raw.replace("\\\"", "\"");
        }
        return raw;
    }

    /**
     * 从原始文本提取 {"files":[...]} JSON 片段。
     * 处理模型输出 markdown 代码块或前后附加说明文字的情况。
     */
    private String extractJsonFragment(String text) {
        String cleaned = text
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();

        int start = cleaned.indexOf("{\"files\"");
        if (start < 0) start = cleaned.indexOf("{ \"files\"");
        if (start < 0) return null;

        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = start; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            if (escape)                { escape = false; continue; }
            if (c == '\\' && inString) { escape = true;  continue; }
            if (c == '"')              { inString = !inString; continue; }
            if (!inString) {
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) return cleaned.substring(start, i + 1);
                }
            }
        }

        int end = cleaned.lastIndexOf('}');
        return (end > start) ? cleaned.substring(start, end + 1) : null;
    }

    // ─────────────────────────────────────────────────────────────
    // 工具方法
    // ─────────────────────────────────────────────────────────────

    private GeneratedCode failWith(GenerationWorkflowContext ctx, String reason) {
        ctx.setCodeSuccess(false);
        ctx.setFailureReason(reason);
        return GeneratedCode.builder().build();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
