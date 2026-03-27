package com.mordan.aihub.lowcode.workflow.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mordan.aihub.lowcode.ai.IntentCheckAiService;
import com.mordan.aihub.lowcode.workflow.state.IntentCheckResult;
import com.mordan.aihub.lowcode.workflow.state.WorkflowState;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 意图检查节点
 * 判断用户输入是否包含生成Web网站/前端的意图
 */
@Slf4j
@Component
public class IntentCheckNode implements NodeAction<WorkflowState> {

    @Resource
    private IntentCheckAiService intentCheckAiService;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public Map<String, Object> apply(WorkflowState state) {
        String userPrompt = state.context().getUserPrompt();
        IntentCheckResult result;
        try {
            String jsonResult = intentCheckAiService.checkIntent(userPrompt).trim();
            try {
                result = objectMapper.readValue(jsonResult, IntentCheckResult.class);
            } catch (Exception e) {
                log.warn("Failed to parse intent check result, defaulting to hasIntent=true", e);
                result = IntentCheckResult.builder()
                        .hasIntent(false)
                        .reason("解析失败")
                        .build();
            }
            log.info("Intent check completed, hasIntent: {}, reason: {}", result.getHasIntent(), result.getReason());
        } catch (Exception e) {
            log.error("Failed to check intent", e);
            result = IntentCheckResult.builder()
                    .hasIntent(false)
                    .reason("意图检查失败：" + e.getMessage())
                    .build();
        }
        state.context().setIntentCheckResult(result);
        return WorkflowState.saveContext(state.context());
    }
}
