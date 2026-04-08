package com.mordan.aihub.lowcode.workflow.node;

import com.mordan.aihub.lowcode.config.LowCodeProperties;
import com.mordan.aihub.lowcode.constant.AppConstant;
import com.mordan.aihub.lowcode.infrastructure.sse.SseEmitterRegistry;
import com.mordan.aihub.lowcode.workflow.build.VueProjectBuilder;
import com.mordan.aihub.lowcode.workflow.state.GenerationWorkflowContext;
import com.mordan.aihub.lowcode.workflow.state.WorkflowState;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.io.IOException;
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
    private SseEmitterRegistry sseEmitterRegistry;
    @Resource
    private LowCodeProperties lowCodeProperties;

    @Override
    public Map<String, Object> apply(WorkflowState state) {
        GenerationWorkflowContext ctx = state.context();
        String generatedResult = ctx.getGeneratedResult();
        int retryCount = ctx.getRetryCount() == null ? 0 : ctx.getRetryCount();

        if (generatedResult == null) {
            log.warn("No generated code to build");
            return WorkflowState.saveContext(ctx);
        }

        sseEmitterRegistry.sendProgress(ctx.getTaskId(), "build", "正在编译构建前端项目", retryCount);

        Path buildDir = null;
        try {
            // 1. 获取构建目录前缀（已在任务提交时生成），创建构建目录
            String buildDirPrefix = ctx.getBuildDirPrefix();
            buildDir = createBuildDirectory(buildDirPrefix);
            log.info("Build starting, build directory: {}", buildDir);

            // 2. 所有文件（包括全新项目）都已经通过工具直接写入构建目录
            // 不需要在这里重复写入，统一使用工具方式保证一致性

            // 3. 使用 VueProjectBuilder 构建项目
            boolean buildSuccess = vueProjectBuilder.buildProject(buildDir.toString());
            if (!buildSuccess) {
                log.error("Vue project build failed");
                ctx.setSuccess(false);
                ctx.setFailureReason("前端项目构建失败，请检查生成代码");
                return WorkflowState.saveContext(ctx);
            }

            // 4. dist 编译产物已经输出到磁盘，不需要保存到 GeneratedCode

            log.info("Frontend build completed successfully, prefix={}", buildDirPrefix);

        } catch (Exception e) {
            log.error("Build failed with exception", e);
            ctx.setSuccess(false);
            ctx.setFailureReason("代码编译异常：" + e.getMessage());
        }

        return WorkflowState.saveContext(ctx);
    }

    /**
     * 创建构建目录
     * 每次生成使用随机前缀，隔离不同构建
     */
    private Path createBuildDirectory(String buildDirPrefix) throws IOException {
        String dirName = AppConstant.CODE_OUTPUT_PREFIX + buildDirPrefix;
        Path basePath = Path.of(lowCodeProperties.getCodeOutputRootDir());
        Files.createDirectories(basePath);
        Path projectDir = basePath.resolve(dirName);
        if (!Files.exists(projectDir)) {
            Files.createDirectories(projectDir);
        }
        return projectDir;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

}
