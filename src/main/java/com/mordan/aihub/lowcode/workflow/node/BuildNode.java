package com.mordan.aihub.lowcode.workflow.node;

import com.mordan.aihub.lowcode.config.GenerationProperties;
import com.mordan.aihub.lowcode.workflow.build.VueProjectBuilder;
import com.mordan.aihub.lowcode.workflow.state.GeneratedCode;
import com.mordan.aihub.lowcode.workflow.state.GenerationWorkflowContext;
import com.mordan.aihub.lowcode.workflow.state.WorkflowState;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

/**
 * 构建编译节点
 * 使用 VueProjectBuilder 构建完整 Vue 项目
 */
@Slf4j
@Component
public class BuildNode implements NodeAction<WorkflowState> {

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private GenerationProperties generationProperties;

    @Override
    public Map<String, Object> apply(WorkflowState state) {
        GenerationWorkflowContext ctx = state.context();
        GeneratedCode generatedCode = ctx.getGeneratedCode();

        if (generatedCode == null || generatedCode.getFiles() == null) {
            log.warn("No generated code to build");
            return WorkflowState.saveContext(ctx);
        }

        Path buildDir = null;
        try {
            // 1. 创建构建目录
            buildDir = createBuildDirectory();
            log.info("Build starting, build directory: {}", buildDir);

            // 2. 写入所有源码文件
            writeSourceFiles(buildDir, generatedCode);

            // 3. 使用 VueProjectBuilder 构建项目
            boolean buildSuccess = vueProjectBuilder.buildProject(buildDir.toString());
            if (!buildSuccess) {
                log.error("Vue project build failed");
                ctx.setSuccess(false);
                ctx.setFailureReason("前端项目构建失败，请检查生成代码");
                cleanupBuildDirectory(buildDir);
                return WorkflowState.saveContext(ctx);
            }

            // 4. 读取编译输出，更新到 GeneratedCode
            updateGeneratedCode(generatedCode, buildDir);

            // 5. 根据配置决定是否清理构建目录
            if (generationProperties.isPersistBuildOutput()) {
                log.info("persist-build-output is enabled, build directory is preserved at: {}", buildDir);
                // 将持久化路径保存到上下文供后续使用
                ctx.setPersistedBuildPath(buildDir.toString());
            } else {
                cleanupBuildDirectory(buildDir);
            }

            log.info("Frontend build completed successfully");

        } catch (Exception e) {
            log.error("Build failed with exception", e);
            ctx.setSuccess(false);
            ctx.setFailureReason("代码编译异常：" + e.getMessage());
            if (buildDir != null && !generationProperties.isPersistBuildOutput()) {
                cleanupBuildDirectory(buildDir);
            }
        }

        return WorkflowState.saveContext(ctx);
    }

    /**
     * 创建构建目录
     * 如果开启持久化，则在指定基础路径下创建，否则使用系统临时目录
     */
    private Path createBuildDirectory() throws IOException {
        String dirName = "lowcode-build-" + UUID.randomUUID();
        if (generationProperties.isPersistBuildOutput() && generationProperties.getPersistOutputBasePath() != null) {
            Path basePath = Path.of(generationProperties.getPersistOutputBasePath());
            Files.createDirectories(basePath);
            return basePath.resolve(dirName);
        }
        return Files.createTempDirectory("lowcode-build-");
    }

    private void writeSourceFiles(Path buildDir, GeneratedCode generatedCode) throws IOException {
        for (var file : generatedCode.getFiles()) {
            Path filePath = buildDir.resolve(file.getPath());
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, file.getContent(), StandardCharsets.UTF_8);
        }
    }

    private void updateGeneratedCode(GeneratedCode generatedCode, Path buildDir) throws IOException {
        Path distDir = buildDir.resolve("dist");
        if (Files.exists(distDir)) {
            Files.walk(distDir)
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            String relativePath = distDir.relativize(file).toString();
                            String content = Files.readString(file, StandardCharsets.UTF_8);
                            // 查找并更新原有文件
                            boolean found = false;
                            for (var existing : generatedCode.getFiles()) {
                                if (existing.getPath().endsWith(relativePath)) {
                                    existing.setContent(content);
                                    found = true;
                                    break;
                                }
                            }
                            // 如果是新文件，添加进去
                            if (!found) {
                                generatedCode.getFiles().add(new com.mordan.aihub.lowcode.workflow.state.CodeFile(
                                        "dist/" + relativePath,
                                        content
                                ));
                            }
                        } catch (IOException e) {
                            log.warn("Failed to read output file: {}", file, e);
                        }
                    });
        }
    }

    /**
     * 清理构建目录，只有在未开启持久化时才清理
     */
    private void cleanupBuildDirectory(Path buildDir) {
        if (generationProperties.isPersistBuildOutput()) {
            return;
        }
        try {
            Files.walk(buildDir).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    // ignore
                }
            });
        } catch (IOException e) {
            // ignore
        }
    }
}
