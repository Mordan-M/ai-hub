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
        String userPrompt = state.context().getUserPrompt();
        IntentCheckResult result = doCheckIntent(userPrompt);
        state.context().setIntentCheckResult(result);
        log.info("Intent check: hasIntent={}, reason={}", result.getHasIntent(), result.getReason());
        return WorkflowState.saveContext(state.context());
    }

    /**
     * 调用 AI 做意图识别，并处理所有异常情况。
     * - AI 调用失败 → 默认 hasIntent=true，避免误拦截正常请求
     * - JSON 解析失败 → 尝试从文本中提取布尔值，仍失败则默认 true
     */
    private IntentCheckResult doCheckIntent(String userPrompt) {
        String rawResult;
        try {
            rawResult = intentCheckAiService.checkIntent(userPrompt).trim();
        } catch (Exception e) {
            log.error("IntentCheckAiService call failed, defaulting hasIntent=true", e);
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
}
