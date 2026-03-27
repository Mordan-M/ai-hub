package com.mordan.aihub.lowcode.workflow.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mordan.aihub.lowcode.ai.RepairCodeAiService;
import com.mordan.aihub.lowcode.workflow.state.GeneratedCode;
import com.mordan.aihub.lowcode.workflow.state.GenerationWorkflowContext;
import com.mordan.aihub.lowcode.workflow.state.WorkflowState;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 代码修复节点
 * 根据一级规则校验的错误列表 + 二级 LLM 建议修复代码。
 * 修复失败时保留原始代码，重试计数仍递增，防止死循环。
 */
@Slf4j
@Component
public class RepairCodeNode implements NodeAction<WorkflowState> {

    @Resource
    private RepairCodeAiService repairCodeAiService;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public Map<String, Object> apply(WorkflowState state) {
        GenerationWorkflowContext ctx = state.context();

        // 递增重试计数（无论修复成功与否，防止无限循环）
        int newRetryCount = (ctx.getRetryCount() == null ? 0 : ctx.getRetryCount()) + 1;
        ctx.setRetryCount(newRetryCount);

        GeneratedCode original = ctx.getGeneratedCode();
        String userPrompt = buildRepairPrompt(ctx);

        GeneratedCode repaired = callRepairAi(userPrompt, original);
        ctx.setGeneratedCode(repaired);

        log.info("Code repair completed, retryCount={}", newRetryCount);
        return WorkflowState.saveContext(ctx);
    }

    // ─────────────────────────────────────────────────────────────
    // 构建修复 Prompt
    // ─────────────────────────────────────────────────────────────

    private String buildRepairPrompt(GenerationWorkflowContext ctx) {
        StringBuilder sb = new StringBuilder();

        // 错误列表（一级规则校验产出，必须修复）
        List<String> errors = ctx.getValidationErrors();
        if (errors != null && !errors.isEmpty()) {
            sb.append("<错误列表>\n");
            errors.forEach(e -> sb.append("- ").append(e).append("\n"));
            sb.append("</错误列表>\n\n");
        }

        // LLM 建议（二级校验产出，酌情采纳）
        if (hasText(ctx.getLlmSuggestions())) {
            sb.append("<优化建议>\n").append(ctx.getLlmSuggestions().trim()).append("\n</优化建议>\n\n");
        }

        // 原始代码 JSON
        try {
            String codeJson = objectMapper.writeValueAsString(ctx.getGeneratedCode());
            sb.append("<原始代码>\n").append(codeJson).append("\n</原始代码>\n");
        } catch (Exception e) {
            log.warn("Failed to serialize generated code for repair prompt", e);
        }

        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────
    // 调用 AI 修复 + JSON 解析（含自动修复）
    // ─────────────────────────────────────────────────────────────

    private GeneratedCode callRepairAi(String userPrompt, GeneratedCode fallback) {
        String rawResult;
        try {
            rawResult = repairCodeAiService.repairCode(userPrompt).trim();
        } catch (Exception e) {
            log.error("RepairCodeAiService call failed, keeping original code", e);
            return fallback;
        }

        // 第一次尝试：直接解析
        try {
            return objectMapper.readValue(rawResult, GeneratedCode.class);
        } catch (Exception firstEx) {
            log.warn("Repair JSON parse failed, attempting extraction. error={}", firstEx.getMessage());
        }

        // 第二次尝试：提取 JSON 片段
        String extracted = extractJsonFragment(rawResult);
        if (extracted != null) {
            try {
                GeneratedCode result = objectMapper.readValue(extracted, GeneratedCode.class);
                log.info("Repair JSON extraction recovery succeeded");
                return result;
            } catch (Exception secondEx) {
                log.error("Repair JSON extraction recovery failed", secondEx);
            }
        }

        log.warn("All repair JSON parse attempts failed, keeping original code");
        return fallback;
    }

    /**
     * 从原始文本中提取 {"files":[...]} 片段，
     * 处理模型在 JSON 前后输出说明文字或 markdown 代码块的情况。
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
            if (escape) { escape = false; continue; }
            if (c == '\\' && inString) { escape = true; continue; }
            if (c == '"') { inString = !inString; continue; }
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
