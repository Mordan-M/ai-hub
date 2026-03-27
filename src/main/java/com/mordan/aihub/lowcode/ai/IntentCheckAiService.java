package com.mordan.aihub.lowcode.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 意图检查 AI 服务
 */
public interface IntentCheckAiService {

    @SystemMessage(fromResource = "prompts/lowcode/intent-check-system-prompt.txt")
    String checkIntent(@UserMessage String userPrompt);
}
