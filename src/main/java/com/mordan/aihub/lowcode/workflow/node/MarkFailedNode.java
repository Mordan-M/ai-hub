package com.mordan.aihub.lowcode.workflow.node;

import com.mordan.aihub.lowcode.domain.entity.GenerationTask;
import com.mordan.aihub.lowcode.domain.enums.TaskStatus;
import com.mordan.aihub.lowcode.mapper.GenerationTaskMapper;
import com.mordan.aihub.lowcode.domain.service.ConversationService;
import com.mordan.aihub.lowcode.infrastructure.sse.SseEmitterRegistry;
import com.mordan.aihub.lowcode.workflow.state.GenerationWorkflowContext;
import com.mordan.aihub.lowcode.workflow.state.WorkflowState;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 标记失败节点
 * 超过最大重试次数后，将任务标记为失败，推送 SSE 错误事件，保存失败消息到对话。
 * 各步骤相互独立，单步失败不影响其他步骤执行。
 */
@Slf4j
@Component
public class MarkFailedNode implements NodeAction<WorkflowState> {

    private static final String DEFAULT_FAILURE_REASON = "代码生成失败";

    @Resource
    private GenerationTaskMapper generationTaskMapper;
    @Resource
    private SseEmitterRegistry sseEmitterRegistry;
    @Resource
    private ConversationService conversationService;

    @Override
    public Map<String, Object> apply(WorkflowState state) {
        GenerationWorkflowContext ctx = state.context();
        String taskId = ctx.getTaskId();
        Long taskIdLong = parseLong(taskId, "taskId");
        String failureReason = ctx.getFailureReason() != null ? ctx.getFailureReason() : DEFAULT_FAILURE_REASON;

        ctx.setSuccess(false);
        ctx.setFailureReason(failureReason);

        // 1. 更新任务状态（数据库）
        updateTaskStatus(taskIdLong, failureReason);

        // 2. 推送 SSE 错误事件
        sendSseError(taskId, failureReason);

        // 3. 保存失败消息到对话
        saveFailureMessage(ctx, taskIdLong, failureReason);

        // 4. 关闭 SSE 连接
        completeSse(taskId);

        log.info("Generation marked as failed: taskId={}, reason={}", taskId, failureReason);
        return WorkflowState.saveContext(ctx);
    }

    private void updateTaskStatus(Long taskIdLong, String failureReason) {
        try {
            GenerationTask task = generationTaskMapper.selectById(taskIdLong);
            if (task != null) {
                task.setStatus(TaskStatus.FAILED);
                task.setErrorMessage(failureReason);
                task.setFinishedAt(System.currentTimeMillis());
                generationTaskMapper.updateById(task);
            } else {
                log.warn("Task not found for id={}", taskIdLong);
            }
        } catch (Exception e) {
            log.error("Failed to update task status to FAILED, taskId={}", taskIdLong, e);
        }
    }

    private void sendSseError(String taskId, String failureReason) {
        try {
            sseEmitterRegistry.sendError(taskId, failureReason);
        } catch (Exception e) {
            log.error("Failed to send SSE error event, taskId={}", taskId, e);
        }
    }

    private void saveFailureMessage(GenerationWorkflowContext ctx, Long taskIdLong, String failureReason) {
        try {
            conversationService.saveAssistantMessage(
                    parseLong(ctx.getUserId(), "userId"),
                    parseLong(ctx.getAppId(), "appId"),
                    "生成失败：" + failureReason,
                    taskIdLong,
                    null
            );
        } catch (Exception e) {
            log.error("Failed to save failure message to conversation, taskId={}", taskIdLong, e);
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
