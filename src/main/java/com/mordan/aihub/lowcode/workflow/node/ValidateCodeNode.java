package com.mordan.aihub.lowcode.workflow.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mordan.aihub.lowcode.ai.ValidateCodeAiService;
import com.mordan.aihub.lowcode.workflow.state.CodeFile;
import com.mordan.aihub.lowcode.workflow.state.GeneratedCode;
import com.mordan.aihub.lowcode.workflow.state.GenerationWorkflowContext;
import com.mordan.aihub.lowcode.workflow.state.WorkflowState;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 代码校验节点
 * 分两级校验：
 *   一级（规则校验）：必要文件存在性、禁止词、图片路径 —— 有错误直接进修复，跳过 LLM
 *   二级（LLM 校验）：一级通过后再做语义/最佳实践检查
 *
 * 注意：技术栈已切换为 Vue 3，校验规则同步更新（src/main.js、src/App.vue、.vue 文件检查）。
 */
@Slf4j
@Component
public class ValidateCodeNode implements NodeAction<WorkflowState> {

    // 必须存在的文件列表（与 generate-code-system-prompt 保持一致）
    private static final Set<String> REQUIRED_FILES = Set.of(
            "package.json",
            "index.html",
            "vite.config.js",
            "tailwind.config.js",
            "postcss.config.js",
            "src/main.js",
            "src/App.vue"
    );

    // 禁止出现的 Node.js 服务端模块
    private static final List<String> FORBIDDEN_MODULES = List.of(
            "require('fs')", "require(\"fs\")",
            "require('path')", "require(\"path\")",
            "require('http')", "require(\"http\")",
            "require('express')", "require(\"express\")",
            "process.env"
    );

    // 禁止的图片来源
    private static final List<String> FORBIDDEN_IMG_PATTERNS = List.of(
            "src=\"./assets", "src='./assets",
            "src=\"/assets",  "src='/assets",
            "src=\"/images",  "src='/images",
            "placeholder.com", "via.placeholder"
    );

    @Resource
    private ValidateCodeAiService validateCodeAiService;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public Map<String, Object> apply(WorkflowState state) {
        GenerationWorkflowContext ctx = state.context();
        List<String> errors = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        GeneratedCode generatedCode = ctx.getGeneratedCode();

        // ── 一级：规则强制性校验 ──────────────────────────────────────
        ruleValidate(generatedCode, ctx, errors);

        // ── 二级：LLM 增强校验（仅一级无错时执行，节省 token）──────────
        if (errors.isEmpty()) {
            llmValidate(generatedCode, suggestions);
        }

        ctx.setValidationErrors(errors);
        ctx.setLlmSuggestions(String.join("\n", suggestions));

        log.info("Validation completed: {} errors, {} suggestions", errors.size(), suggestions.size());
        return WorkflowState.saveContext(ctx);
    }

    // ─────────────────────────────────────────────────────────────
    // 一级：规则校验
    // ─────────────────────────────────────────────────────────────

    private void ruleValidate(GeneratedCode generatedCode, GenerationWorkflowContext ctx, List<String> errors) {
        if (generatedCode == null || generatedCode.getFiles() == null) {
            errors.add("格式错误：缺少 files 文件列表");
            return;
        }

        List<CodeFile> files = generatedCode.getFiles();

        // 1. 必要文件存在性检查
        Set<String> presentPaths = new java.util.HashSet<>();
        for (CodeFile f : files) {
            if (f.getPath() != null) presentPaths.add(f.getPath());
        }
        for (String required : REQUIRED_FILES) {
            if (!presentPaths.contains(required)) {
                errors.add("缺少必要文件：" + required);
            }
        }

        // 2. 逐文件内容检查
        for (CodeFile file : files) {
            String path = file.getPath();
            String content = file.getContent();
            if (path == null || content == null) continue;

            // 2a. 禁止服务端模块
            for (String forbidden : FORBIDDEN_MODULES) {
                if (content.contains(forbidden)) {
                    errors.add(path + "：包含禁止的服务端模块引用 " + forbidden);
                }
            }

            // 2b. 禁止本地图片路径 / 占位图服务
            for (String pattern : FORBIDDEN_IMG_PATTERNS) {
                if (content.contains(pattern)) {
                    errors.add(path + "：包含禁止的图片路径或占位图服务 " + pattern);
                }
            }

            // 2c. Vue 文件：确认使用 <script setup>（组合式 API）
            if (path.endsWith(".vue") && content.contains("<script") && !content.contains("<script setup")) {
                errors.add(path + "：Vue 组件未使用 <script setup> 语法，请改用组合式 API");
            }

            // 2d. package.json：禁止 "latest" 版本
            if ("package.json".equals(path) && content.contains("\"latest\"")) {
                errors.add("package.json：存在 \"latest\" 版本号，请指定精确版本");
            }

            // 2e. Tailwind 配置：content 路径必须覆盖 .vue 文件
            if ("tailwind.config.js".equals(path) && !content.contains(".vue")) {
                errors.add("tailwind.config.js：content 未覆盖 .vue 文件，Tailwind 样式将被 purge 清空");
            }
        }

        // 3. 有 API 文档时，检查是否包含接口调用代码
        if (hasText(ctx.getApiDocText())) {
            boolean foundApiCall = files.stream().anyMatch(f -> {
                if (f.getContent() == null) return false;
                // 检查是否有 fetch( 或 window.API_BASE_URL 的引用
                return f.getContent().contains("fetch(") || f.getContent().contains("API_BASE_URL");
            });
            if (!foundApiCall) {
                errors.add("提供了 API 文档但未检测到接口调用代码（fetch / API_BASE_URL）");
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 二级：LLM 校验
    // ─────────────────────────────────────────────────────────────

    private void llmValidate(GeneratedCode generatedCode, List<String> suggestions) {
        String codeJson;
        try {
            codeJson = objectMapper.writeValueAsString(generatedCode);
        } catch (Exception e) {
            log.warn("Failed to serialize code for LLM validation", e);
            return;
        }

        try {
            String llmResult = validateCodeAiService.validateCode(codeJson).trim();
            if ("无".equals(llmResult) || llmResult.isEmpty()) return;

            for (String line : llmResult.split("\n")) {
                String trimmed = line.trim().replaceAll("^[-*\\d.]+\\s*", "");
                if (!trimmed.isEmpty()) {
                    suggestions.add(trimmed);
                }
            }
            log.info("LLM validation found {} suggestions", suggestions.size());
        } catch (Exception e) {
            log.warn("LLM validation failed, skipping", e);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
