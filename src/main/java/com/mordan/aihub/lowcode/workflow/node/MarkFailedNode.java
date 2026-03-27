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
 * 超过最大重试次数后标记任务失败
 */
@Slf4j
@Component
public class MarkFailedNode implements NodeAction<WorkflowState> {

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
        Long taskIdLong = Long.parseLong(taskId);
        String failureReason = ctx.getFailureReason();

        if (failureReason == null) {
            failureReason = "超过最大重试次数，代码校验仍不通过";
        }

        // 更新上下文
        ctx.setSuccess(false);
        ctx.setFailureReason(failureReason);

        try {
            // 更新任务状态
            GenerationTask task = generationTaskMapper.selectById(taskIdLong);
            if (task != null) {
                task.setStatus(TaskStatus.FAILED);
                task.setErrorMessage(failureReason);
                task.setFinishedAt(System.currentTimeMillis());
                generationTaskMapper.updateById(task);
            }

            // 推送错误事件
            sseEmitterRegistry.sendError(taskId, failureReason);

            // 保存失败消息到对话
            conversationService.saveAssistantMessage(
                    Long.parseLong(ctx.getUserId()),
                    Long.parseLong(ctx.getAppId()),
                    "生成失败：" + failureReason,
                    taskIdLong,
                    null
            );

            // 完成SSE连接
            sseEmitterRegistry.complete(taskId);

            log.info("Generation marked as failed: taskId={}, reason={}", taskId, failureReason);
        } catch (Exception e) {
            log.error("Failed to mark generation as failed", e);
            // 仍然保存失败状态到上下文，让工作流可以正常结束
        }

        return WorkflowState.saveContext(ctx);
    }
}
