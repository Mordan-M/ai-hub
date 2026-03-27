package com.mordan.aihub.lowcode.workflow.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mordan.aihub.lowcode.ai.RepairCodeAiService;
import com.mordan.aihub.lowcode.workflow.state.GeneratedCode;
import com.mordan.aihub.lowcode.workflow.state.GenerationWorkflowContext;
import com.mordan.aihub.lowcode.workflow.state.WorkflowState;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 代码修复节点
 * 根据校验错误列表修复代码
 */
@Slf4j
@Component
public class RepairCodeNode implements NodeAction<WorkflowState> {

    @Resource
    private RepairCodeAiService repairCodeAiService;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public Map<String, Object> apply(WorkflowState state) {
        GenerationWorkflowContext ctx = state.context();
        Integer currentRetry = ctx.getRetryCount();
        int newRetryCount = (currentRetry == null ? 0 : currentRetry) + 1;

        List<String> errors = ctx.getValidationErrors();
        GeneratedCode generatedCode = ctx.getGeneratedCode();
        GeneratedCode repairedCode = generatedCode;

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("错误列表：\n");
        if (errors != null) {
            for (String error : errors) {
                userPrompt.append("- ").append(error).append("\n");
            }
        }
        // 序列化为JSON传给AI
        String generatedCodeJson;
        try {
            generatedCodeJson = objectMapper.writeValueAsString(generatedCode);
        } catch (Exception e) {
            log.warn("Failed to serialize generated code JSON, keeping original", e);
            generatedCodeJson = "";
        }
        userPrompt.append("\n原始代码：\n").append(generatedCodeJson);

        try {
            String repairedJson = repairCodeAiService.repairCode(userPrompt.toString()).trim();
            try {
                repairedCode = objectMapper.readValue(repairedJson, GeneratedCode.class);
            } catch (Exception e) {
                log.warn("Failed to parse repaired code JSON, keeping original", e);
                repairedCode = generatedCode;
            }
            log.info("Code repair completed, retry count: {}", newRetryCount);
        } catch (Exception e) {
            log.error("Failed to repair code", e);
            repairedCode = generatedCode;
        }

        // 更新上下文
        ctx.setGeneratedCode(repairedCode);
        ctx.setRetryCount(newRetryCount);

        return WorkflowState.saveContext(ctx);
    }
}
