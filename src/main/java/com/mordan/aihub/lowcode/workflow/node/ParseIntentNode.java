package com.mordan.aihub.lowcode.workflow.node;

import com.mordan.aihub.lowcode.ai.ParseIntentAiService;
import com.mordan.aihub.lowcode.workflow.state.GenerationWorkflowContext;
import com.mordan.aihub.lowcode.workflow.state.WorkflowState;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 意图解析节点
 * 将用户自然语言需求解析为结构化JSON
 */
@Slf4j
@Component
public class ParseIntentNode implements NodeAction<WorkflowState> {

    @Resource
    private ParseIntentAiService parseIntentAiService;

    @Override
    public Map<String, Object> apply(WorkflowState state) {
        GenerationWorkflowContext ctx = state.context();
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("用户需求：\n").append(ctx.getUserPrompt()).append("\n\n");

        if (ctx.getApiDocText() != null && !ctx.getApiDocText().isEmpty()) {
            userPrompt.append("API文档：\n").append(ctx.getApiDocText()).append("\n\n");
        }

        if (ctx.getParentCodeSnapshot() != null && !ctx.getParentCodeSnapshot().isEmpty()) {
            userPrompt.append("此为迭代修改，请基于已有代码进行改动。\n");
        }

        String parsedIntent;
        try {
            parsedIntent = parseIntentAiService.parseIntent(userPrompt.toString()).trim();
            log.info("Intent parsing completed");
        } catch (Exception e) {
            log.error("Failed to parse intent", e);
            parsedIntent = ctx.getUserPrompt();
            ctx.setSuccess(false);
            ctx.setFailureReason("意图解析失败：" + e.getMessage());
        }
        ctx.setParsedIntent(parsedIntent);
        return WorkflowState.saveContext(ctx);
    }
}
