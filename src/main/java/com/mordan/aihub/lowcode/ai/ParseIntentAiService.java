package com.mordan.aihub.lowcode.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 意图解析 AI 服务
 */
public interface ParseIntentAiService {

    @SystemMessage(fromResource = "prompts/lowcode/parse-intent-system-prompt.txt")
    String parseIntent(@UserMessage String userPrompt);
}
