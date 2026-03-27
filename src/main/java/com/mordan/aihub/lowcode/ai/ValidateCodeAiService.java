package com.mordan.aihub.lowcode.ai;

import dev.langchain4j.service.UserMessage;

/**
 * 代码校验 AI 服务
 */
public interface ValidateCodeAiService {

    @UserMessage(fromResource = "prompts/lowcode/validate-code-user-prompt.txt")
    String validateCode(String generatedCode);
}
