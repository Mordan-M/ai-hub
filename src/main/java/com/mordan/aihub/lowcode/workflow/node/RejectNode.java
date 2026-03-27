package com.mordan.aihub.lowcode.workflow.node;

import com.mordan.aihub.lowcode.domain.entity.GenerationTask;
import com.mordan.aihub.lowcode.domain.enums.TaskStatus;
import com.mordan.aihub.lowcode.mapper.GenerationTaskMapper;
import com.mordan.aihub.lowcode.domain.service.ConversationService;
import com.mordan.aihub.lowcode.infrastructure.sse.SseEmitterRegistry;
import com.mordan.aihub.lowcode.workflow.state.GenerationWorkflowContext;
import com.mordan.aihub.lowcode.workflow.state.IntentCheckResult;
import com.mordan.aihub.lowcode.workflow.state.WorkflowState;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 拒绝节点
 * 意图检查不通过时，标记任务失败并向用户推送友好引导提示。
 * 各步骤相互独立，单步失败不影响其他步骤执行。
 *
 * 优化点：移除未使用的 ObjectMapper 依赖；各 I/O 操作独立 try-catch，
 * 避免一个步骤失败导致 SSE 连接未关闭。
 */
@Slf4j
@Component
public class RejectNode implements NodeAction<WorkflowState> {

    private static final String REJECT_GUIDE_MESSAGE =
            "您的输入暂不包含生成网站的意图，请描述您希望生成的网站内容，" +
            "例如：「帮我生成一个电商首页」或「制作一个餐厅介绍页面」。";

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

        IntentCheckResult intentResult = ctx.getIntentCheckResult();
        String reason = (intentResult != null && hasText(intentResult.getReason()))
                ? intentResult.getReason()
                : "无法识别意图";

        ctx.setSuccess(false);
        ctx.setFailureReason("用户意图与生成网站无关：" + reason);

        // 1. 更新任务状态（数据库）
        updateTaskStatus(taskIdLong, reason);

        // 2. 推送 SSE 拒绝事件（含用户引导文案）
        sendSseRejected(taskId, reason);

        // 3. 保存拒绝消息到对话
        saveRejectedMessage(ctx, taskIdLong, reason);

        // 4. 关闭 SSE 连接
        completeSse(taskId);

        log.info("Generation rejected: taskId={}, reason={}", taskId, reason);
        return WorkflowState.saveContext(ctx);
    }

    private void updateTaskStatus(Long taskIdLong, String reason) {
        try {
            GenerationTask task = generationTaskMapper.selectById(taskIdLong);
            if (task != null) {
                task.setStatus(TaskStatus.FAILED);
                task.setErrorMessage("意图识别失败：" + reason);
                task.setFinishedAt(System.currentTimeMillis());
                generationTaskMapper.updateById(task);
            } else {
                log.warn("Task not found for id={}", taskIdLong);
            }
        } catch (Exception e) {
            log.error("Failed to update task status to FAILED, taskId={}", taskIdLong, e);
        }
    }

    private void sendSseRejected(String taskId, String reason) {
        try {
            sseEmitterRegistry.sendRejected(taskId, REJECT_GUIDE_MESSAGE, reason);
        } catch (Exception e) {
            log.error("Failed to send SSE rejected event, taskId={}", taskId, e);
        }
    }

    private void saveRejectedMessage(GenerationWorkflowContext ctx, Long taskIdLong, String reason) {
        try {
            conversationService.saveAssistantMessage(
                    parseLong(ctx.getUserId(), "userId"),
                    parseLong(ctx.getAppId(), "appId"),
                    "生成已拒绝：" + reason,
                    taskIdLong,
                    null
            );
        } catch (Exception e) {
            log.error("Failed to save rejected message to conversation, taskId={}", taskIdLong, e);
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
