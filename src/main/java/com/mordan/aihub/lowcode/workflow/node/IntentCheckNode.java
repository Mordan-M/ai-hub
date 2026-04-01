package com.mordan.aihub.lowcode.workflow.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mordan.aihub.lowcode.ai.IntentCheckAiService;
import com.mordan.aihub.lowcode.workflow.state.GenerationWorkflowContext;
import com.mordan.aihub.lowcode.workflow.state.IntentCheckResult;
import com.mordan.aihub.lowcode.workflow.state.WorkflowState;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 意图检查节点
 * 判断用户输入是否包含生成 Web 网站/前端的意图。
 * 解析失败时默认 hasIntent=true（宽松策略：宁可多生成，不误拦截）。
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
        GenerationWorkflowContext ctx = state.context();
        String appId = ctx.getAppId();
        String userPrompt = ctx.getUserPrompt();
        String existingProjectSummary = ctx.getExistingProjectSummary();

        // 当已有项目摘要存在时，追加到 prompt 中，告知 AI 这是修改意图检查
        String fullPrompt = buildFullPrompt(userPrompt, existingProjectSummary);
        IntentCheckResult result = doCheckIntent(appId, fullPrompt);
        ctx.setIntentCheckResult(result);
        log.info("Intent check: hasIntent={}, reason={}", result.getHasIntent(), result.getReason());
        return WorkflowState.saveContext(ctx);
    }

    /**
     * 构建完整 prompt，当已有项目摘要存在时追加说明
     */
    private String buildFullPrompt(String userPrompt, String existingProjectSummary) {
        StringBuilder sb = new StringBuilder();
        sb.append(userPrompt);
        sb.append("\n\n");

        // 如果已有项目摘要存在，说明这是迭代修改任务，需要检查修改意图
        if (hasText(existingProjectSummary)) {
            sb.append("=====\n");
            sb.append("已有项目文件摘要（供你参考判断修改范围）：\n");
            sb.append(existingProjectSummary);
            sb.append("\n=====\n");
            sb.append("请判断上述用户需求是否是对该已有项目的合理修改需求。");
        }

        return sb.toString();
    }

    /**
     * 调用 AI 做意图识别，并处理所有异常情况。
     * - AI 调用失败 → 默认 hasIntent=true，避免误拦截正常请求
     * - JSON 解析失败 → 尝试从文本中提取布尔值，仍失败则默认 true
     */
    private IntentCheckResult doCheckIntent(String appId, String userPrompt) {
        String rawResult;
        try {
            rawResult = intentCheckAiService.checkIntent(appId, userPrompt).trim();
        } catch (Exception e) {
            log.error("Intent check call failed, defaulting hasIntent=true", e);
            return IntentCheckResult.builder().hasIntent(true).reason("服务调用失败，默认放行").build();
        }

        // 第一次尝试：直接解析
        try {
            return objectMapper.readValue(rawResult, IntentCheckResult.class);
        } catch (Exception firstEx) {
            log.warn("Intent JSON parse failed, attempting text extraction. raw={}", rawResult);
        }

        // 第二次尝试：从文本中粗匹配布尔值
        boolean hasIntent = !rawResult.contains("\"hasIntent\":false") && !rawResult.contains("\"hasIntent\": false");
        log.warn("Intent check fallback extraction: hasIntent={}", hasIntent);
        return IntentCheckResult.builder().hasIntent(hasIntent).reason("JSON解析失败，降级处理").build();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
