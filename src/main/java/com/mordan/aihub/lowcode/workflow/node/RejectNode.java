package com.mordan.aihub.lowcode.workflow.node;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 当用户意图不包含生成网站时，拒绝请求并记录
 */
@Slf4j
@Component
public class RejectNode implements NodeAction<WorkflowState> {

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
        String taskId = ctx.getTaskId();
        IntentCheckResult intentResult = ctx.getIntentCheckResult();

        String reason = "无法识别意图";
        if (intentResult != null && intentResult.getReason() != null) {
            reason = intentResult.getReason();
        }

        // 更新上下文
        ctx.setSuccess(false);
        ctx.setFailureReason("用户意图与生成网站无关：" + reason);

        try {
            // 更新任务状态为失败
            Long taskIdLong = Long.parseLong(taskId);
            GenerationTask task = generationTaskMapper.selectById(taskIdLong);
            if (task != null) {
                task.setStatus(TaskStatus.FAILED);
                task.setErrorMessage("意图识别失败：" + reason);
                task.setFinishedAt(System.currentTimeMillis());
                generationTaskMapper.updateById(task);
            }

            // 通过SSE推送拒绝消息
            sseEmitterRegistry.sendRejected(taskId,
                    "您的输入暂不包含生成网站的意图，请描述您希望生成的网站内容，例如：帮我生成一个电商首页。",
                    reason);

            // 保存拒绝消息到对话
            conversationService.saveAssistantMessage(
                    Long.parseLong(ctx.getUserId()),
                    Long.parseLong(ctx.getAppId()),
                    "生成已拒绝：" + reason,
                    taskIdLong,
                    null
            );

            // 完成SSE连接
            sseEmitterRegistry.complete(taskId);

            log.info("Generation rejected, reason: {}", reason);
        } catch (Exception e) {
            log.error("Failed to mark generation as rejected", e);
            // 仍然保存失败状态到上下文，让工作流可以正常结束
        }

        return WorkflowState.saveContext(ctx);
    }
}
