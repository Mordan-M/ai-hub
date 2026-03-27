package com.mordan.aihub.lowcode.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 代码生成 AI 服务
 */
public interface GenerateCodeAiService {

    @SystemMessage(fromResource = "prompts/lowcode/generate-code-system-prompt.txt")
    String generateCode(@UserMessage String userPrompt);
}
