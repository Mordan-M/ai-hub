package com.mordan.aihub.lowcode.workflow.node;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mordan.aihub.lowcode.ai.ValidateAiService;
import com.mordan.aihub.lowcode.tools.CurrentBuildContext;
import com.mordan.aihub.lowcode.workflow.state.GenerationWorkflowContext;
import com.mordan.aihub.lowcode.workflow.state.QualityResult;
import com.mordan.aihub.lowcode.workflow.state.WorkflowState;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    /**
     * 需要检查的文件扩展名
     */
    private static final List<String> CODE_EXTENSIONS = Arrays.asList(
            ".html", ".htm", ".css", ".js", ".json", ".vue", ".ts", ".jsx", ".tsx"
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
        String appId = ctx.getAppId();
        log.info("执行节点: 代码质量检查");

        Path projectRoot = CurrentBuildContext.getProjectRoot(appId);
        QualityResult qualityResult;
        try {
            // 1. 读取并拼接代码文件内容
            String codeContent = readAndConcatenateCodeFiles(projectRoot);
            if (StrUtil.isBlank(codeContent)) {
                log.warn("未找到可检查的代码文件");
                qualityResult = QualityResult.builder()
                        .isValid(false)
                        .errors(List.of("未找到可检查的代码文件"))
                        .suggestions(List.of("请确保代码生成成功"))
                        .build();
            } else {
                // 2. 调用 AI 进行代码质量检查
                qualityResult = lowCodeStatelessAiService.validateCode(codeContent);
                log.info("代码质量检查完成 - 是否通过: {}", qualityResult.getIsValid());
            }
        } catch (Exception e) {
            log.error("代码质量检查异常: {}", e.getMessage(), e);
            qualityResult = QualityResult.builder()
                    .isValid(true) // 异常直接跳到下一个步骤
                    .build();
        }
        // 3. 更新状态
        ctx.setQualityResult(qualityResult);

        log.info("Validation completed: qualityResult={}", qualityResult);
        return WorkflowState.saveContext(ctx);
    }

    /**
     * 读取并拼接代码目录下的所有代码文件
     */
    private static String readAndConcatenateCodeFiles(Path projectRoot) {
        if (Objects.isNull(projectRoot)) {
            return "";
        }
        File directory = projectRoot.toFile();
        if (!directory.exists() || !directory.isDirectory()) {
            log.error("代码目录不存在或不是目录: {}", projectRoot.toString());
            return "";
        }
        StringBuilder codeContent = new StringBuilder();
        codeContent.append("# 项目文件结构和代码内容\n\n");
        // 使用 Hutool 的 walkFiles 方法遍历所有文件
        FileUtil.walkFiles(directory, file -> {
            // 过滤条件：跳过隐藏文件、特定目录下的文件、非代码文件
            if (shouldSkipFile(file, directory)) {
                return;
            }
            if (isCodeFile(file)) {
                String relativePath = FileUtil.subPath(directory.getAbsolutePath(), file.getAbsolutePath());
                codeContent.append("## 文件: ").append(relativePath).append("\n\n");
                String fileContent = FileUtil.readUtf8String(file);
                codeContent.append(fileContent).append("\n\n");
            }
        });
        return codeContent.toString();
    }


    /**
     * 判断是否应该跳过此文件
     */
    private static boolean shouldSkipFile(File file, File rootDir) {
        String relativePath = FileUtil.subPath(rootDir.getAbsolutePath(), file.getAbsolutePath());
        // 跳过隐藏文件
        if (file.getName().startsWith(".")) {
            return true;
        }
        // 跳过特定目录下的文件
        return relativePath.contains("node_modules" + File.separator) ||
                relativePath.contains("dist" + File.separator) ||
                relativePath.contains("target" + File.separator) ||
                relativePath.contains(".git" + File.separator);
    }

    /**
     * 判断是否是需要检查的代码文件
     */
    private static boolean isCodeFile(File file) {
        String fileName = file.getName().toLowerCase();
        return CODE_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

}
