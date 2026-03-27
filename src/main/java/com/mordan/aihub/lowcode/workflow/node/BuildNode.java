package com.mordan.aihub.lowcode.workflow.node;

import com.mordan.aihub.lowcode.workflow.state.GeneratedCode;
import com.mordan.aihub.lowcode.workflow.state.GenerationWorkflowContext;
import com.mordan.aihub.lowcode.workflow.state.WorkflowState;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * 构建编译节点
 * 使用 esbuild 编译 Vue 3 JS/TS 为浏览器可执行的 JavaScript
 */
@Slf4j
@Component
public class BuildNode implements NodeAction<WorkflowState> {

    private static final String ESBUILD_CMD = "npx";
    private static final String ESBUILD_ARGS = "esbuild";

    @Override
    public Map<String, Object> apply(WorkflowState state) {
        GenerationWorkflowContext ctx = state.context();
        GeneratedCode generatedCode = ctx.getGeneratedCode();

        if (generatedCode == null || generatedCode.getFiles() == null) {
            log.warn("No generated code to build");
            return WorkflowState.saveContext(ctx);
        }

        try {
            // 1. 创建临时目录存放源码
            Path tempDir = Files.createTempDirectory("lowcode-build-");
            log.info("Build starting, temp directory: {}", tempDir);

            // 2. 写入所有源码文件
            writeSourceFiles(tempDir, generatedCode);

            // 3. 查找入口文件 index.html 提取 entry point
            String entryPoint = findEntryPoint(tempDir);
            if (entryPoint == null) {
                log.warn("No entry point found (index.html with /src/main.js or /src/main.ts)");
                Files.walk(tempDir).forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        // ignore
                    }
                });
                return WorkflowState.saveContext(ctx);
            }

            // 4. 执行 esbuild 编译
            boolean buildSuccess = runEsbuild(tempDir, entryPoint);
            if (!buildSuccess) {
                log.error("esbuild compilation failed");
                ctx.setSuccess(false);
                ctx.setFailureReason("前端代码编译失败，请检查生成代码");
                Files.walk(tempDir).forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        // ignore
                    }
                });
                return WorkflowState.saveContext(ctx);
            }

            // 5. 读取编译输出，更新到 GeneratedCode
            updateGeneratedCode(generatedCode, tempDir);

            // 6. 清理临时目录
            Files.walk(tempDir).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    // ignore
                }
            });

            log.info("Frontend build completed successfully");

        } catch (Exception e) {
            log.error("Build failed with exception", e);
            ctx.setSuccess(false);
            ctx.setFailureReason("代码编译异常：" + e.getMessage());
        }

        return WorkflowState.saveContext(ctx);
    }

    private void writeSourceFiles(Path tempDir, GeneratedCode generatedCode) throws IOException {
        for (var file : generatedCode.getFiles()) {
            Path filePath = tempDir.resolve(file.getPath());
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, file.getContent(), StandardCharsets.UTF_8);
        }
    }

    private String findEntryPoint(Path tempDir) {
        try {
            Path indexHtml = tempDir.resolve("index.html");
            if (!Files.exists(indexHtml)) {
                return null;
            }
            String content = Files.readString(indexHtml);
            // 查找 src/main.js 或 src/main.ts
            if (content.contains("/src/main.js")) {
                return "src/main.js";
            }
            if (content.contains("/src/main.ts")) {
                return "src/main.ts";
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    private boolean runEsbuild(Path tempDir, String entryPoint) {
        String outputFile = "dist/main.js";
        Path outputPath = tempDir.resolve(outputFile);
        try {
            Files.createDirectories(outputPath.getParent());
        } catch (IOException e) {
            log.error("Failed to create output directory", e);
            return false;
        }

        // esbuild 命令
        ProcessBuilder pb = new ProcessBuilder(
                ESBUILD_CMD,
                ESBUILD_ARGS,
                entryPoint,
                "--bundle",
                "--outfile=" + outputFile,
                "--format=iife",
                "--sourcemap"
        );
        pb.directory(tempDir.toFile());
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            // 读取输出
            InputStream inputStream = process.getInputStream();
            String output = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                log.error("esbuild exited with code {}, output:\n{}", exitCode, output);
                return false;
            }

            log.info("esbuild output:\n{}", output);
            return Files.exists(outputPath);

        } catch (IOException | InterruptedException e) {
            log.error("Failed to run esbuild", e);
            return false;
        }
    }

    private void updateGeneratedCode(GeneratedCode generatedCode, Path tempDir) throws IOException {
        Path distDir = tempDir.resolve("dist");
        if (Files.exists(distDir)) {
            Files.walk(distDir)
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            String relativePath = distDir.relativize(file).toString();
                            String content = Files.readString(file, StandardCharsets.UTF_8);
                            // 查找并更新原有文件
                            for (var existing : generatedCode.getFiles()) {
                                if (existing.getPath().endsWith(relativePath)) {
                                    existing.setContent(content);
                                    return;
                                }
                            }
                            // 如果是新文件（如 source map），添加进去
                            generatedCode.getFiles().add(new com.mordan.aihub.lowcode.workflow.state.CodeFile(
                                    "dist/" + relativePath,
                                    content
                            ));
                        } catch (IOException e) {
                            log.warn("Failed to read output file: {}", file, e);
                        }
                    });
        }
        // 更新 index.html 中入口路径指向编译后的 dist/main.js
        updateIndexHtmlEntryPoint(tempDir, generatedCode);
    }

    private void updateIndexHtmlEntryPoint(Path tempDir, GeneratedCode generatedCode) {
        for (var file : generatedCode.getFiles()) {
            if ("index.html".equals(file.getPath())) {
                String content = file.getContent();
                // 将 src/main.js / src/main.ts 替换为 dist/main.js
                content = content.replace("/src/main.js", "/dist/main.js");
                content = content.replace("/src/main.ts", "/dist/main.js");
                file.setContent(content);
                break;
            }
        }
    }
}
