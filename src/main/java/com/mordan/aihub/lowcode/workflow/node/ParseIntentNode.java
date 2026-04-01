package com.mordan.aihub.lowcode.workflow.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mordan.aihub.lowcode.ai.ParseIntentAiService;
import com.mordan.aihub.lowcode.workflow.state.GenerationWorkflowContext;
import com.mordan.aihub.lowcode.workflow.state.ParsedIntent;
import com.mordan.aihub.lowcode.workflow.state.WorkflowState;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 意图解析节点
 * 将用户自然语言需求解析为结构化 POJO ParsedIntent。
 * 解析失败时将原始用户输入作为 fallback，并标记失败原因。
 */
@Slf4j
@Component
public class ParseIntentNode implements NodeAction<WorkflowState> {

    @Resource
    private ParseIntentAiService parseIntentAiService;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public Map<String, Object> apply(WorkflowState state) {
        GenerationWorkflowContext ctx = state.context();
        String appId = ctx.getAppId();
        String userPrompt = buildUserPrompt(ctx);

        try {
            String jsonResult = parseIntentAiService.parseIntent(appId, userPrompt).trim();
            // 解析 JSON 为 POJO
            ParsedIntent parsedIntent = objectMapper.readValue(jsonResult, ParsedIntent.class);
            ctx.setParsedIntent(parsedIntent);
            ctx.setParsedIntentJson(jsonResult);
            log.info("Intent parsing completed, pages: {}, hasApi: {}",
                    parsedIntent.getPages() != null ? parsedIntent.getPages().size() : 0,
                    parsedIntent.getHasApiIntegration());
        } catch (Exception e) {
            log.error("Failed to parse intent", e);
            // 降级：保存原始 JSON 字符串到 parsedIntentJson，POJO 为 null
            ctx.setParsedIntentJson(ctx.getUserPrompt());
            ctx.setSuccess(false);
            ctx.setFailureReason("意图解析失败：" + e.getMessage());
        }

        return WorkflowState.saveContext(ctx);
    }

    /**
     * 组装结构化 userPrompt，使用 XML 标签包裹各段内容，
     * 帮助模型准确定位不同语义区块。
     */
    private String buildUserPrompt(GenerationWorkflowContext ctx) {
        StringBuilder sb = new StringBuilder();

        // 用户需求（必填）
        appendSection(sb, "用户需求", ctx.getUserPrompt());

        // API 文档（可选）
        if (hasText(ctx.getApiDocText())) {
            appendSection(sb, "API文档", ctx.getApiDocText());
        }

        // 已有项目文件摘要（可选，迭代修改时存在）
        if (hasText(ctx.getExistingProjectSummary())) {
            appendSection(sb, "已有项目文件摘要",
                    "这是一个已有项目，请根据用户需求在该项目基础上进行修改。" +
                    "以下是该项目的文件结构摘要，请参考它来理解现有结构并进行修改：\n" +
                    ctx.getExistingProjectSummary());
            appendSection(sb, "迭代说明", "此次为迭代修改任务，请将 isIteration 设为 true。" +
                    "修改时只改动用户需求中明确要求修改的部分，保持原有结构不变。");
        }
        return sb.toString();
    }

    private void appendSection(StringBuilder sb, String tag, String content) {
        sb.append("<").append(tag).append(">\n")
          .append(content == null ? "" : content.trim())
          .append("\n</").append(tag).append(">\n\n");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
