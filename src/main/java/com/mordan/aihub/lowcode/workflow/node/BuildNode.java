package com.mordan.aihub.lowcode.workflow.node;

import com.mordan.aihub.lowcode.config.GenerationProperties;
import com.mordan.aihub.lowcode.constant.AppConstant;
import com.mordan.aihub.lowcode.workflow.build.VueProjectBuilder;
import com.mordan.aihub.lowcode.workflow.state.GeneratedCode;
import com.mordan.aihub.lowcode.workflow.state.GenerationWorkflowContext;
import com.mordan.aihub.lowcode.workflow.state.WorkflowState;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

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
            buildDir = createBuildDirectory(ctx.getAppId());
            log.info("Build starting, build directory: {}", buildDir);

            // 2. 写入所有源码文件
            writeSourceFiles(buildDir, generatedCode);

            // 3. 使用 VueProjectBuilder 构建项目
            boolean buildSuccess = vueProjectBuilder.buildProject(buildDir.toString());
            if (!buildSuccess) {
                log.error("Vue project build failed");
                ctx.setSuccess(false);
                ctx.setFailureReason("前端项目构建失败，请检查生成代码");
                return WorkflowState.saveContext(ctx);
            }

            // 4. 读取编译输出，更新到 GeneratedCode
            updateGeneratedCode(generatedCode, buildDir);

            log.info("Frontend build completed successfully");

        } catch (Exception e) {
            log.error("Build failed with exception", e);
            ctx.setSuccess(false);
            ctx.setFailureReason("代码编译异常：" + e.getMessage());
//            if (buildDir != null && !generationProperties.isPersistBuildOutput()) {
//                cleanupBuildDirectory(buildDir);
//            }
        }

        return WorkflowState.saveContext(ctx);
    }

    /**
     * 创建构建目录
     */
    private Path createBuildDirectory(String appId) throws IOException {
        String dirName = "lowcode-output-" + appId;
        Path basePath = Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR);
//        if (Objects.nonNull(generationProperties.getPersistOutputBasePath())) {
//            basePath = Path.of(generationProperties.getPersistOutputBasePath());
//        }
        Files.createDirectories(basePath);
        return basePath.resolve(dirName);
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

}
