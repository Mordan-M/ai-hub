package com.mordan.aihub.lowcode.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * @className: IntentCheckAiService
 * @description: 意图检查 AI 服务接口
 * @author: 91002183
 * @date: 2026/3/31
 **/
public interface IntentCheckAiService {

    /**
     * 意图检查：判断用户输入是否包含生成网站的意图
     *
     * @param userPrompt 用户输入
     * @return JSON 结果 {"hasIntent": true/false, "reason": "说明"}
     */
    @SystemMessage(fromResource = "prompts/lowcode/intent-check-system-prompt.txt")
    String checkIntent(@MemoryId String appId, @UserMessage String userPrompt);

}
