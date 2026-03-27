package com.mordan.aihub.lowcode.workflow.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mordan.aihub.lowcode.domain.entity.Application;
import com.mordan.aihub.lowcode.domain.entity.GeneratedVersion;
import com.mordan.aihub.lowcode.domain.entity.GenerationTask;
import com.mordan.aihub.lowcode.domain.enums.TaskStatus;
import com.mordan.aihub.lowcode.domain.enums.VersionDeployStatus;
import com.mordan.aihub.lowcode.mapper.ApplicationMapper;
import com.mordan.aihub.lowcode.mapper.GeneratedVersionMapper;
import com.mordan.aihub.lowcode.mapper.GenerationTaskMapper;
import com.mordan.aihub.lowcode.domain.service.ConversationService;
import com.mordan.aihub.lowcode.infrastructure.sse.SseEmitterRegistry;
import com.mordan.aihub.lowcode.infrastructure.storage.FileStorageService;
import com.mordan.aihub.lowcode.workflow.state.GeneratedCode;
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
 * 保存版本节点
 * 将通过校验的代码保存到文件系统和数据库
 */
@Slf4j
@Component
public class SaveVersionNode implements NodeAction<WorkflowState> {

    @Resource
    private ApplicationMapper applicationMapper;
    @Resource
    private GeneratedVersionMapper generatedVersionMapper;
    @Resource
    private GenerationTaskMapper generationTaskMapper;
    @Resource
    private FileStorageService fileStorageService;
    @Resource
    private SseEmitterRegistry sseEmitterRegistry;
    @Resource
    private ConversationService conversationService;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public Map<String, Object> apply(WorkflowState state) {
        GenerationWorkflowContext ctx = state.context();
        Long appId = Long.parseLong(ctx.getAppId());
        Long userId = Long.parseLong(ctx.getUserId());
        Long taskId = Long.parseLong(ctx.getTaskId());
        GeneratedCode generatedCode = ctx.getGeneratedCode() != null ? ctx.getGeneratedCode() : ctx.getFinalCode();
        String userPrompt = ctx.getUserPrompt();

        // 1. 查询当前应用最大版本号
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GeneratedVersion> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(GeneratedVersion::getAppId, appId)
               .orderByDesc(GeneratedVersion::getVersionNumber)
               .last("LIMIT 1");
        GeneratedVersion latestVersion = generatedVersionMapper.selectOne(wrapper);
        Integer maxVersion = latestVersion != null ? latestVersion.getVersionNumber() : null;
        int newVersionNumber = (maxVersion == null ? 0 : maxVersion) + 1;

        // 2. 写入文件系统 - 需要序列化为JSON
        String storagePath;
        try {
            String generatedCodeJson = objectMapper.writeValueAsString(generatedCode);
            storagePath = fileStorageService.writeVersion(userId, appId, newVersionNumber, generatedCodeJson);
        } catch (Exception e) {
            log.error("Failed to write version files", e);
            ctx.setSuccess(false);
            ctx.setFailureReason("文件写入失败：" + e.getMessage());
            return WorkflowState.saveContext(ctx);
        }

        // 3. 计算文件总大小
        long totalSize = calculateTotalSize(storagePath);

        // 4. 写入GeneratedVersion记录
        GeneratedVersion version = GeneratedVersion.builder()
                .appId(appId)
                .taskId(taskId)
                .versionNumber(newVersionNumber)
                .codeStoragePath(storagePath)
                .previewUrl("/preview/" + appId + "/" + newVersionNumber)
                .downloadUrl("/api/apps/" + appId + "/versions/" + taskId + "/download")
                .fileSize(totalSize)
                .validationResult(null)
                .promptSnapshot(userPrompt)
                .deployStatus(VersionDeployStatus.NOT_DEPLOYED)
                .build();
        generatedVersionMapper.insert(version);

        // 5. 更新Application最新版本ID
        Application app = applicationMapper.selectById(appId);
        app.setLatestVersionId(version.getId());
        app.setUpdatedAt(System.currentTimeMillis());
        applicationMapper.updateById(app);

        // 6. 更新任务状态为成功
        GenerationTask task = generationTaskMapper.selectById(taskId);
        task.setStatus(TaskStatus.SUCCESS);
        task.setFinishedAt(System.currentTimeMillis());
        generationTaskMapper.updateById(task);

        // 7. 更新上下文
        ctx.setFinalCode(generatedCode);
        ctx.setSuccess(true);

        // 8. 推送完成事件
        String previewUrl = "/preview/" + version.getId();
        sseEmitterRegistry.sendComplete(ctx.getTaskId(), version.getId(), previewUrl);

        // 9. 保存成功消息到对话
        conversationService.saveAssistantMessage(
                userId,
                appId,
                "代码生成完成，版本 " + newVersionNumber,
                taskId,
                version.getId()
        );

        // 10. 完成SSE连接
        sseEmitterRegistry.complete(ctx.getTaskId());

        log.info("Version saved successfully: appId={}, versionNumber={}, versionId={}",
                appId, newVersionNumber, version.getId());

        return WorkflowState.saveContext(ctx);
    }

    /**
     * 计算目录总文件大小
     */
    private long calculateTotalSize(String storagePath) {
        try {
            return Files.walk(Path.of(storagePath))
                    .filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .sum();
        } catch (IOException e) {
            return 0;
        }
    }
}
