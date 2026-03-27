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
 * 将通过校验的代码写入文件系统和数据库，推送完成事件，保存成功消息。
 *
 * 优化点：
 *   1. Long.parseLong 统一提取为工具方法，避免重复 try-catch
 *   2. app / task 的 null 检查前置，避免 NPE
 *   3. 各 I/O 操作独立 try-catch，部分失败不影响主流程
 *   4. previewUrl 统一使用 version.getId()，与原逻辑保持一致
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
        Long appId  = parseLong(ctx.getAppId(),  "appId");
        Long userId = parseLong(ctx.getUserId(), "userId");
        Long taskId = parseLong(ctx.getTaskId(), "taskId");

        // 优先取 generatedCode，降级取 finalCode
        GeneratedCode generatedCode = ctx.getGeneratedCode() != null
                ? ctx.getGeneratedCode()
                : ctx.getFinalCode();

        // 1. 计算新版本号
        int newVersionNumber = resolveNextVersionNumber(appId);

        // 2. 写入文件系统
        String storagePath = writeToStorage(ctx, userId, appId, newVersionNumber, generatedCode);
        if (storagePath == null) {
            // writeToStorage 内部已设置 failureReason
            return WorkflowState.saveContext(ctx);
        }

        // 3. 计算文件总大小
        long totalSize = calculateTotalSize(storagePath);

        // 4. 写入 GeneratedVersion 记录
        GeneratedVersion version = GeneratedVersion.builder()
                .appId(appId)
                .taskId(taskId)
                .versionNumber(newVersionNumber)
                .codeStoragePath(storagePath)
                .previewUrl("/preview/" + appId + "/" + newVersionNumber)
                .downloadUrl("/api/apps/" + appId + "/versions/" + taskId + "/download")
                .fileSize(totalSize)
                .validationResult(null)
                .promptSnapshot(ctx.getUserPrompt())
                .deployStatus(VersionDeployStatus.NOT_DEPLOYED)
                .build();
        generatedVersionMapper.insert(version);

        // 5. 更新 Application 最新版本 ID
        updateApplication(appId, version.getId());

        // 6. 更新任务状态为成功
        updateTaskSuccess(taskId);

        // 7. 更新上下文
        ctx.setFinalCode(generatedCode);
        ctx.setSuccess(true);

        // 8. 推送 SSE 完成事件
        String previewUrl = "/preview/" + version.getId();
        sendSseComplete(ctx.getTaskId(), version.getId(), previewUrl);

        // 9. 保存成功消息到对话
        saveSuccessMessage(userId, appId, taskId, newVersionNumber, version.getId());

        // 10. 关闭 SSE 连接
        completeSse(ctx.getTaskId());

        log.info("Version saved: appId={}, versionNumber={}, versionId={}",
                appId, newVersionNumber, version.getId());
        return WorkflowState.saveContext(ctx);
    }

    // ─────────────────────────────────────────────────────────────
    // 私有方法
    // ─────────────────────────────────────────────────────────────

    /** 查询当前 App 的最大版本号，返回下一个版本号 */
    private int resolveNextVersionNumber(Long appId) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GeneratedVersion> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(GeneratedVersion::getAppId, appId)
               .orderByDesc(GeneratedVersion::getVersionNumber)
               .last("LIMIT 1");
        GeneratedVersion latest = generatedVersionMapper.selectOne(wrapper);
        return (latest != null && latest.getVersionNumber() != null ? latest.getVersionNumber() : 0) + 1;
    }

    /** 将代码序列化后写入文件系统，返回存储路径；失败时设置 ctx 错误信息并返回 null */
    private String writeToStorage(GenerationWorkflowContext ctx,
                                  Long userId, Long appId, int versionNumber,
                                  GeneratedCode generatedCode) {
        try {
            String codeJson = objectMapper.writeValueAsString(generatedCode);
            return fileStorageService.writeVersion(userId, appId, versionNumber, codeJson);
        } catch (Exception e) {
            log.error("Failed to write version files", e);
            ctx.setSuccess(false);
            ctx.setFailureReason("文件写入失败：" + e.getMessage());
            return null;
        }
    }

    /** 递归计算目录下所有文件的总字节数 */
    private long calculateTotalSize(String storagePath) {
        try {
            return Files.walk(Path.of(storagePath))
                    .filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try { return Files.size(p); } catch (IOException e) { return 0L; }
                    })
                    .sum();
        } catch (IOException e) {
            log.warn("Failed to calculate total size for path={}", storagePath);
            return 0L;
        }
    }

    private void updateApplication(Long appId, Long latestVersionId) {
        try {
            Application app = applicationMapper.selectById(appId);
            if (app != null) {
                app.setLatestVersionId(latestVersionId);
                app.setUpdatedAt(System.currentTimeMillis());
                applicationMapper.updateById(app);
            } else {
                log.warn("Application not found for appId={}", appId);
            }
        } catch (Exception e) {
            log.error("Failed to update application latestVersionId, appId={}", appId, e);
        }
    }

    private void updateTaskSuccess(Long taskId) {
        try {
            GenerationTask task = generationTaskMapper.selectById(taskId);
            if (task != null) {
                task.setStatus(TaskStatus.SUCCESS);
                task.setFinishedAt(System.currentTimeMillis());
                generationTaskMapper.updateById(task);
            } else {
                log.warn("Task not found for taskId={}", taskId);
            }
        } catch (Exception e) {
            log.error("Failed to update task status to SUCCESS, taskId={}", taskId, e);
        }
    }

    private void sendSseComplete(String taskId, Long versionId, String previewUrl) {
        try {
            sseEmitterRegistry.sendComplete(taskId, versionId, previewUrl);
        } catch (Exception e) {
            log.error("Failed to send SSE complete event, taskId={}", taskId, e);
        }
    }

    private void saveSuccessMessage(Long userId, Long appId, Long taskId,
                                    int versionNumber, Long versionId) {
        try {
            conversationService.saveAssistantMessage(
                    userId, appId,
                    "代码生成完成，版本 " + versionNumber,
                    taskId, versionId
            );
        } catch (Exception e) {
            log.error("Failed to save success message, taskId={}", taskId, e);
        }
    }

    private void completeSse(String taskId) {
        try {
            sseEmitterRegistry.complete(taskId);
        } catch (Exception e) {
            log.error("Failed to complete SSE connection, taskId={}", taskId, e);
        }
    }

    private Long parseLong(String value, String fieldName) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.error("Invalid {} value: {}", fieldName, value);
            throw new IllegalArgumentException("Invalid " + fieldName + ": " + value);
        }
    }
}
