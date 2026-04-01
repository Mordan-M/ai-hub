package com.mordan.aihub.lowcode.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * @className: ParseIntentAiService
 * @description: 意图解析 AI 服务接口
 * @author: 91002183
 * @date: 2026/3/31
 **/
public interface ParseIntentAiService {

    /**
     * 意图解析：将自然语言需求解析为结构化 JSON
     * @param appId 应用ID（用于记忆）
     * @param userPrompt 用户输入
     * @return 结构化 JSON
     */
    @SystemMessage(fromResource = "prompts/lowcode/parse-intent-system-prompt.txt")
    String parseIntent(@MemoryId String appId, @UserMessage String userPrompt);


}
