package com.mordan.aihub.lowcode.workflow.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mordan.aihub.lowcode.constant.AppConstant;
import com.mordan.aihub.lowcode.domain.entity.GeneratedRecord;
import com.mordan.aihub.lowcode.domain.entity.GenerationTask;
import com.mordan.aihub.lowcode.domain.enums.TaskStatus;
import com.mordan.aihub.lowcode.domain.service.ConversationService;
import com.mordan.aihub.lowcode.infrastructure.sse.SseEmitterRegistry;
import com.mordan.aihub.lowcode.mapper.GeneratedRecordMapper;
import com.mordan.aihub.lowcode.mapper.GenerationTaskMapper;
import com.mordan.aihub.lowcode.workflow.state.GeneratedResult;
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
 * 保留所有生成记录，不再追踪最新版本概念。
 */
@Slf4j
@Component
public class SaveGenerateRecordNode implements NodeAction<WorkflowState> {

    @Resource
    private GeneratedRecordMapper generatedVersionMapper;
    @Resource
    private GenerationTaskMapper generationTaskMapper;
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
        GeneratedResult generatedResult = ctx.getGeneratedResult();

        // 代码已经在构建阶段写入文件系统，直接使用构建目录路径
        String buildDirPrefix = ctx.getBuildDirPrefix();
        String dirName = AppConstant.CODE_OUTPUT_PREFIX + buildDirPrefix;
        String storagePath = Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR, dirName).toString();

        // 计算文件总大小
        long totalSize = calculateTotalSize(storagePath);

        // 删除该应用旧记录，插入新记录（每个 appId 只保留最新一条）
        deleteExistingVersion(appId);
        // 将结构化摘要序列化为 JSON 字符串保存到数据库
        String projectSummaryJson = null;
        if (generatedResult.getSummary() != null) {
            try {
                projectSummaryJson = objectMapper.writeValueAsString(generatedResult.getSummary());
            } catch (Exception e) {
                log.warn("Failed to serialize project summary", e);
            }
        }

        // 设置访问地址并更新记录
        String previewUrl = AppConstant.PREVIEW_URL_PREFIX + appId;
        String downloadUrl = AppConstant.DOWNLOAD_URL_PREFIX + appId;

        GeneratedRecord version = GeneratedRecord.builder()
                .appId(appId)
                .taskId(taskId)
                .filePrefix(buildDirPrefix)
                .codeStoragePath(storagePath)
                .fileSize(totalSize)
                .promptSnapshot(ctx.getUserPrompt())
                .projectSummary(projectSummaryJson)
                .previewUrl(previewUrl)
                .downloadUrl(downloadUrl)
                .build();
        generatedVersionMapper.insert(version);

        // 5. 更新任务状态为成功
        updateTaskSuccess(taskId);

        // 7. 更新上下文
        ctx.setSuccess(true);

        // 8. 推送 SSE 完成事件
        sendSseComplete(ctx.getTaskId(), version.getId(), previewUrl);

        // 9. 保存成功消息到对话
        saveSuccessMessage(userId, appId, taskId);

        // 10. 关闭 SSE 连接
        completeSse(ctx.getTaskId());

        log.info("Code saved: appId={}, hasSummary={}", appId, generatedResult.getSummary() != null);
        return WorkflowState.saveContext(ctx);
    }

    // ─────────────────────────────────────────────────────────────
    // 私有方法
    // ─────────────────────────────────────────────────────────────

    /** 删除该应用已存在的记录，每个 appId 只保留最新一条 */
    private void deleteExistingVersion(Long appId) {
        try {
            generatedVersionMapper.delete(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GeneratedRecord>()
                            .eq(GeneratedRecord::getAppId, appId)
            );
        } catch (Exception e) {
            log.warn("Failed to delete existing version for appId={}, {}", appId, e.getMessage());
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

    private void saveSuccessMessage(Long userId, Long appId, Long taskId) {
        try {
            conversationService.saveAssistantMessage(
                    userId, appId,
                    "代码生成完成",
                    taskId
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
