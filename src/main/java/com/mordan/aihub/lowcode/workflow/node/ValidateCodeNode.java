package com.mordan.aihub.lowcode.workflow.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mordan.aihub.lowcode.ai.ValidateAiService;
import com.mordan.aihub.lowcode.workflow.state.CodeFile;
import com.mordan.aihub.lowcode.workflow.state.GeneratedCode;
import com.mordan.aihub.lowcode.workflow.state.GenerationWorkflowContext;
import com.mordan.aihub.lowcode.workflow.state.WorkflowState;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 代码校验节点
 *
 * 分两级校验，职责严格分离：
 *
 * 【一级：Java 规则校验】— 守门员，结果触发修复流程
 *   检查"有没有"，不检查"好不好"。100% 确定性，零 token 消耗。
 *   - 必要文件存在性
 *   - 禁止服务端模块引用
 *   - 禁止本地图片路径 / 占位图服务
 *   - Vue 文件必须使用 <script setup>
 *   - vite.config.js 必须包含 @vitejs/plugin-vue
 *   - src/main.js 必须有 mount() 调用
 *   - index.html 必须有 id="app" 挂载点
 *   - Vue Router 使用时必须在 main.js 注册
 *   - package.json 禁止 "latest" 版本
 *   - tailwind.config.js content 必须覆盖 .vue 文件
 *   - 有 API 文档时必须有接口调用代码
 *
 * 【二级：LLM 语义校验】— 质检员，结果注入下次生成，不触发修复
 *   只传核心业务文件（src/App.vue、src/views/**、src/components/**），
 *   降低 token 消耗。结果存入 ctx.llmSuggestions，
 *   由 GenerateCodeNode 在下一轮生成时作为质检反馈注入 prompt。
 */
@Slf4j
@Component
public class ValidateCodeNode implements NodeAction<WorkflowState> {

    // ── 常量 ──────────────────────────────────────────────────────

    private static final Set<String> REQUIRED_FILES = Set.of(
            "package.json",
            "index.html",
            "vite.config.js",
            "src/main.js",
            "src/App.vue",
            "src/router/index.js"   // prompt 明确要求必须有路由文件
    );

    private static final List<String> FORBIDDEN_MODULES = List.of(
            "require('fs')",      "require(\"fs\")",
            "require('path')",    "require(\"path\")",
            "require('http')",    "require(\"http\")",
            "require('express')", "require(\"express\")",
            "process.env"
    );

    private static final List<String> FORBIDDEN_IMG_PATTERNS = List.of(
            "src=\"./assets", "src='./assets",
            "src=\"/assets",  "src='/assets",
            "src=\"/images",  "src='/images",
            "placeholder.com", "via.placeholder"
    );

    // LLM 校验只传这些路径前缀的文件，降低 token 消耗
    private static final List<String> LLM_VALIDATE_PREFIXES = List.of(
            "src/App.vue",
            "src/views/",
            "src/components/"
    );

    @Resource
    private ValidateAiService lowCodeStatelessAiService;

    @Resource
    private ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────────
    // 主入口
    // ─────────────────────────────────────────────────────────────

    @Override
    public Map<String, Object> apply(WorkflowState state) {
        GenerationWorkflowContext ctx = state.context();
        GeneratedCode generatedCode = ctx.getGeneratedCode();

        List<String> errors = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        // 一级：规则强制校验（有错误直接进修复，跳过 LLM）
        ruleValidate(generatedCode, ctx, errors);

        // 二级：LLM 语义校验（仅一级无错时执行，只传业务文件）
        if (errors.isEmpty()) {
            llmValidate(generatedCode, suggestions);
        }

        ctx.setValidationErrors(errors);
        // LLM suggestions 保留在上下文，由 GenerateCodeNode 下一轮注入 prompt
        ctx.setLlmSuggestions(String.join("\n", suggestions));

        log.info("Validation completed: {} rule errors, {} llm suggestions", errors.size(), suggestions.size());
        return WorkflowState.saveContext(ctx);
    }

    // ─────────────────────────────────────────────────────────────
    // 一级：Java 规则校验
    // ─────────────────────────────────────────────────────────────

    private void ruleValidate(GeneratedCode generatedCode,
                               GenerationWorkflowContext ctx,
                               List<String> errors) {
        if (generatedCode == null || generatedCode.getFiles() == null) {
            errors.add("格式错误：缺少 files 文件列表");
            return;
        }

        List<CodeFile> files = generatedCode.getFiles();
        Map<String, String> fileMap = buildFileMap(files);

        boolean isIterative = hasText(ctx.getAppId()) && hasText(ctx.getExistingProjectSummary());
        checkRequiredFiles(fileMap, errors, isIterative);
        checkFileContents(files, errors);
        checkVueRouterRegistration(fileMap, errors, isIterative);
        checkApiIntegration(ctx, files, errors, isIterative);
    }

    /** 构建 path → content 的 Map，便于快速查找 */
    private Map<String, String> buildFileMap(List<CodeFile> files) {
        Map<String, String> map = new HashMap<>();
        for (CodeFile f : files) {
            if (f.getPath() != null && f.getContent() != null) {
                map.put(f.getPath(), f.getContent());
            }
        }
        return map;
    }

    /** 检查必要文件是否全部存在 */
    private void checkRequiredFiles(Map<String, String> fileMap, List<String> errors, boolean isIterative) {
        for (String required : REQUIRED_FILES) {
            if (!fileMap.containsKey(required) && !isIterative) {
                // 全新项目：必须所有必需文件都存在
                errors.add("缺少必要文件：" + required);
            }
            // 迭代场景：只修改部分文件，不强制所有必需文件都在本次输出中
        }
    }

    /** 逐文件内容规则检查 */
    private void checkFileContents(List<CodeFile> files, List<String> errors) {
        for (CodeFile file : files) {
            String path    = file.getPath();
            String content = file.getContent();
            if (path == null || content == null) continue;

            // 禁止服务端模块
            for (String forbidden : FORBIDDEN_MODULES) {
                if (content.contains(forbidden)) {
                    errors.add(path + "：包含禁止的服务端模块引用 [" + forbidden + "]");
                }
            }

            // 禁止本地图片路径 / 占位图服务
            for (String pattern : FORBIDDEN_IMG_PATTERNS) {
                if (content.contains(pattern)) {
                    errors.add(path + "：包含禁止的图片路径或占位图服务 [" + pattern + "]");
                }
            }

            // Vue 文件必须使用 <script setup>
            if (path.endsWith(".vue") && content.contains("<script") && !content.contains("<script setup")) {
                errors.add(path + "：Vue 组件未使用 <script setup> 语法");
            }

            // vite.config.js 必须引入 @vitejs/plugin-vue
            if ("vite.config.js".equals(path) && !content.contains("@vitejs/plugin-vue")) {
                errors.add("vite.config.js：缺少 @vitejs/plugin-vue 插件，Vue 文件无法编译");
            }

            // vite.config.js 必须配置 base: './'（支持子路径部署）
            if ("vite.config.js".equals(path) && !content.contains("base:")) {
                errors.add("vite.config.js：缺少 base 配置，构建产物无法在子路径下访问");
            }

            // src/main.js 必须有 mount() 调用
            if ("src/main.js".equals(path) && !content.contains("mount(")) {
                errors.add("src/main.js：未找到 mount() 调用，应用无法启动");
            }

            // index.html 必须有 id="app" 挂载点
            if ("index.html".equals(path) && !content.contains("id=\"app\"")) {
                errors.add("index.html：缺少 id=\"app\" 挂载点，Vue 应用无法挂载");
            }

            // package.json 禁止 "latest" 版本
            if ("package.json".equals(path) && content.contains("\"latest\"")) {
                errors.add("package.json：存在 \"latest\" 版本号，请指定精确版本");
            }

        }
    }

    /**
     * Vue Router 使用检查：
     * 若项目包含 src/router/index.js，则 src/main.js 必须调用 app.use(router)
     */
    private void checkVueRouterRegistration(Map<String, String> fileMap, List<String> errors, boolean isIterative) {
        boolean hasRouterInCurrentOutput = fileMap.containsKey("src/router/index.js");
        if (!hasRouterInCurrentOutput) {
            // 全新项目：如果需要 router 必须输出 router 文件
            // 迭代场景：router 文件未修改，不输出也没关系，跳过检查
            return;
        }

        String mainJs = fileMap.get("src/main.js");
        if (mainJs == null && !isIterative) {
            errors.add("src/main.js：项目包含 Vue Router 但未找到 src/main.js");
            return;
        }
        if (mainJs != null && !mainJs.contains("use(router)")) {
            errors.add("src/main.js：项目包含 Vue Router 但未调用 app.use(router)");
        }
        // 迭代场景：main.js 不在当前输出中表示没修改，不检查
    }

    /** 有 API 文档时，必须包含接口调用代码 */
    private void checkApiIntegration(GenerationWorkflowContext ctx,
                                      List<CodeFile> files,
                                      List<String> errors,
                                     boolean isIterative) {
        if (isIterative) {
            return;
        }
        if (!hasText(ctx.getApiDocText())) return;

        boolean foundApiCall = files.stream()
                .filter(f -> f.getContent() != null)
                .anyMatch(f -> f.getContent().contains("fetch(")
                            || f.getContent().contains("API_BASE_URL"));
        if (!foundApiCall) {
            errors.add("提供了 API 文档但未检测到接口调用代码（fetch / API_BASE_URL）");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 二级：LLM 语义校验
    // ─────────────────────────────────────────────────────────────

    /**
     * 只把核心业务文件（App.vue、views、components）传给 LLM，
     * 跳过 package.json、vite.config.js 等配置文件，
     * 大幅降低 token 消耗（通常减少 60%~80%）。
     */
    private void llmValidate(GeneratedCode generatedCode, List<String> suggestions) {
        List<CodeFile> keyFiles = generatedCode.getFiles().stream()
                .filter(f -> f.getPath() != null && LLM_VALIDATE_PREFIXES.stream()
                        .anyMatch(prefix -> f.getPath().startsWith(prefix)
                                        || f.getPath().equals(prefix)))
                .collect(Collectors.toList());

        if (keyFiles.isEmpty()) {
            log.warn("LLM validation skipped: no key business files found");
            return;
        }

        String keyFilesJson;
        try {
            // 只序列化业务文件，而非整个项目
            keyFilesJson = objectMapper.writeValueAsString(
                    Map.of("files", keyFiles)
            );
        } catch (Exception e) {
            log.warn("Failed to serialize key files for LLM validation", e);
            return;
        }

        try {
            String llmResult = lowCodeStatelessAiService.validateCode(keyFilesJson).trim();
            if ("无".equals(llmResult) || llmResult.isEmpty()) {
                log.info("LLM validation: no suggestions");
                return;
            }

            for (String line : llmResult.split("\n")) {
                String trimmed = line.trim().replaceAll("^[-*\\d.]+\\s*", "");
                if (!trimmed.isEmpty()) {
                    suggestions.add(trimmed);
                }
            }
            log.info("LLM validation found {} suggestions", suggestions.size());
        } catch (Exception e) {
            // LLM 校验失败不影响主流程
            log.warn("LLM validation failed, skipping: {}", e.getMessage());
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
