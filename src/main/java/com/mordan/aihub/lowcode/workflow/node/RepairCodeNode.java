package com.mordan.aihub.lowcode.workflow.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mordan.aihub.lowcode.ai.LowCodeGenerateAiService;
import com.mordan.aihub.lowcode.workflow.state.CodeFile;
import com.mordan.aihub.lowcode.workflow.state.GeneratedCode;
import com.mordan.aihub.lowcode.workflow.state.GenerationWorkflowContext;
import com.mordan.aihub.lowcode.workflow.state.WorkflowState;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 代码修复节点（差量修复版）
 *
 * 核心改进（相比全量重写版）：
 *
 * 1. 【差量修复】根据错误列表中的文件路径，只将有问题的文件传给 LLM，
 *    模型只输出修改的文件，再通过 mergeRepaired() patch 合并回原始代码。
 *    → token 减少 80%+，未涉及文件完全保留，不引入新的随机性。
 *
 * 2. 【模板符号转义】将代码中的 {{ }} 替换为 { { } }，
 *    防止 LangChain/LangGraph 框架把 Vue 插值语法当作变量占位符解析。
 *    → 解决 "{{ article.date }} 被框架误识别为变量" 的问题。
 *
 * 3. 【JSON 解析自动修复】解析失败时从原始文本提取 JSON 片段，
 *    处理模型输出 markdown 代码块或在 JSON 前后附加说明文字的情况。
 *
 * 4. 【错误路径收集】从 ValidationError（结构化错误）中提取文件路径，
 *    精准定位需要修复的文件。若 errors 为纯文本（无路径），则降级全量传入。
 */
@Slf4j
@Component
public class RepairCodeNode implements NodeAction<WorkflowState> {

    @Resource
    private LowCodeGenerateAiService lowCodeGenerateAiService;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public Map<String, Object> apply(WorkflowState state) {
        GenerationWorkflowContext ctx = state.context();

        // 重试计数递增（无论修复成功与否，防止死循环）
        int newRetryCount = (ctx.getRetryCount() == null ? 0 : ctx.getRetryCount()) + 1;
        ctx.setRetryCount(newRetryCount);

        GeneratedCode original = ctx.getGeneratedCode();

        // 1. 从错误列表提取涉及的文件路径
        Set<String> errorPaths = extractErrorPaths(ctx.getValidationErrors());

        // 2. 筛选需要修复的文件（无法提取路径时降级全量）
        List<CodeFile> filesToRepair = selectFilesToRepair(original, errorPaths);
        log.info("Repair scope: {}/{} files, errorPaths={}",
                filesToRepair.size(),
                original.getFiles() == null ? 0 : original.getFiles().size(),
                errorPaths);

        // 3. 构建修复 prompt（只含问题文件，并转义模板符号）
        String userPrompt = buildRepairPrompt(ctx, filesToRepair);

        // 4. 调用 AI，获取 patch
        GeneratedCode patch = callRepairAi(userPrompt, original);

        // 5. patch 合并：修复的文件覆盖原始，其余文件完整保留
        GeneratedCode merged = mergeRepaired(original, patch);
        ctx.setGeneratedCode(merged);

        log.info("Code repair completed, retryCount={}, patched {} files",
                newRetryCount,
                patch.getFiles() == null ? 0 : patch.getFiles().size());
        return WorkflowState.saveContext(ctx);
    }

    // ─────────────────────────────────────────────────────────────
    // 差量修复：路径提取 & 文件筛选
    // ─────────────────────────────────────────────────────────────

    /**
     * 从错误信息中提取文件路径。
     * 错误格式约定：以 "文件路径：" 开头（ValidateCodeNode 已按此格式生成）。
     * 无法识别路径的全局错误（如"缺少必要文件"）不产生路径，触发降级全量传入。
     */
    private Set<String> extractErrorPaths(List<String> errors) {
        if (errors == null || errors.isEmpty()) return Collections.emptySet();

        Set<String> paths = new HashSet<>();
        for (String error : errors) {
            // 格式："src/App.vue：Vue 组件未使用 <script setup> 语法"
            int colonIdx = error.indexOf("：");
            if (colonIdx > 0) {
                String candidate = error.substring(0, colonIdx).trim();
                // 简单判断是否像文件路径（包含 / 或 . 且无空格）
                if ((candidate.contains("/") || candidate.contains("."))
                        && !candidate.contains(" ")) {
                    paths.add(candidate);
                }
            }
        }
        return paths;
    }

    /**
     * 根据错误路径筛选需要修复的文件。
     * - 有明确路径 → 只传对应文件（差量）
     * - 无法提取路径（全局错误，如缺少必要文件）→ 降级全量传入
     */
    private List<CodeFile> selectFilesToRepair(GeneratedCode original, Set<String> errorPaths) {
        if (original.getFiles() == null) return Collections.emptyList();

        // 无法确定具体文件时，降级全量
        if (errorPaths.isEmpty()) {
            log.debug("No specific error paths extracted, falling back to full repair");
            return original.getFiles();
        }

        List<CodeFile> targeted = original.getFiles().stream()
                .filter(f -> errorPaths.contains(f.getPath()))
                .collect(Collectors.toList());

        // 目标文件为空（路径不匹配），也降级全量
        return targeted.isEmpty() ? original.getFiles() : targeted;
    }

    // ─────────────────────────────────────────────────────────────
    // 构建修复 Prompt
    // ─────────────────────────────────────────────────────────────

    private String buildRepairPrompt(GenerationWorkflowContext ctx, List<CodeFile> filesToRepair) {
        StringBuilder sb = new StringBuilder();

        // 错误列表（必须修复）
        List<String> errors = ctx.getValidationErrors();
        if (errors != null && !errors.isEmpty()) {
            sb.append("<错误列表>\n");
            errors.forEach(e -> sb.append("- ").append(e).append("\n"));
            sb.append("</错误列表>\n\n");
        }

        // LLM 建议（酌情采纳，不强制）
        if (hasText(ctx.getLlmSuggestions())) {
            sb.append("<优化建议>\n")
              .append(ctx.getLlmSuggestions().trim())
              .append("\n</优化建议>\n\n");
        }

        // 需要修复的文件（已转义模板符号，防止框架误解析）
        try {
            GeneratedCode repairScope = GeneratedCode.builder().files(filesToRepair).build();
            String scopeJson = objectMapper.writeValueAsString(repairScope);
            // 关键：转义 Vue 模板插值符号，防止 LangGraph 框架当变量解析
            String escaped = escapeTemplateDelimiters(scopeJson);
            sb.append("<需要修复的文件>\n")
              .append(escaped)
              .append("\n</需要修复的文件>\n");
        } catch (Exception e) {
            log.warn("Failed to serialize repair scope for prompt", e);
        }

        return sb.toString();
    }

    /**
     * 转义 Vue 模板插值符号。
     * {{ variable }} → { { variable } }
     *
     * 防止 LangChain/LangGraph 框架将 Vue 插值语法误识别为 prompt 变量占位符，
     * 导致 "{{ article.date }}" 被替换为空或报错。
     * 模型仍能理解 { { } } 是 Vue 模板语法，修复时会还原为标准格式。
     */
    private String escapeTemplateDelimiters(String content) {
        return content
                .replace("{{", "{ {")
                .replace("}}", "} }");
    }

    // ─────────────────────────────────────────────────────────────
    // AI 调用 + JSON 解析
    // ─────────────────────────────────────────────────────────────

    private GeneratedCode callRepairAi(String userPrompt, GeneratedCode fallback) {
        String rawResult;
        try {
            rawResult = lowCodeGenerateAiService.repairCode(userPrompt).trim();
        } catch (Exception e) {
            log.error("LowCodeGenerateAiService repairCode call failed, keeping original", e);
            return fallback;
        }

        // 第一次尝试：直接解析
        try {
            return objectMapper.readValue(rawResult, GeneratedCode.class);
        } catch (Exception firstEx) {
            log.warn("Repair JSON parse failed, attempting extraction. error={}", firstEx.getMessage());
        }

        // 第二次尝试：从文本中提取 JSON 片段
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

        log.warn("All repair JSON parse attempts failed, keeping original");
        return fallback;
    }

    /**
     * Patch 合并：将修复后的文件（patch）合并回原始代码（original）。
     * - patch 中存在的文件路径 → 覆盖原始对应文件
     * - patch 中新增的文件路径 → 直接加入
     * - original 中未被 patch 涉及的文件 → 完整保留，不做任何修改
     */
    private GeneratedCode mergeRepaired(GeneratedCode original, GeneratedCode patch) {
        if (patch == null || patch.getFiles() == null || patch.getFiles().isEmpty()) {
            log.warn("Patch is empty, keeping original code");
            return original;
        }

        // 以文件路径为 key 建立索引（LinkedHashMap 保持顺序）
        Map<String, CodeFile> index = new LinkedHashMap<>();
        if (original.getFiles() != null) {
            original.getFiles().forEach(f -> index.put(f.getPath(), f));
        }

        // patch 文件覆盖或新增
        patch.getFiles().forEach(f -> {
            if (f.getPath() != null) {
                index.put(f.getPath(), f);
            }
        });

        return GeneratedCode.builder()
                .files(new ArrayList<>(index.values()))
                .build();
    }

    /**
     * 从原始文本中提取 {"files":[...]} JSON 片段。
     * 处理模型输出 markdown 代码块或在 JSON 前后附加说明的情况。
     * 使用括号深度匹配，正确处理嵌套结构。
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
            if (escape)             { escape = false; continue; }
            if (c == '\\' && inString) { escape = true;  continue; }
            if (c == '"')           { inString = !inString; continue; }
            if (!inString) {
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) return cleaned.substring(start, i + 1);
                }
            }
        }

        // 括号不匹配时退回简单截取
        int end = cleaned.lastIndexOf('}');
        return (end > start) ? cleaned.substring(start, end + 1) : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
