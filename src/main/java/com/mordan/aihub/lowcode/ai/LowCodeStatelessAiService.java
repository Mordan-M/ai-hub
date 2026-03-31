package com.mordan.aihub.lowcode.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 低代码生成 AI 服务
 * 整合意图检查、意图解析、代码生成、代码校验、代码修复所有功能
 */
public interface LowCodeStatelessAiService {

    /**
     * 意图检查：判断用户输入是否包含生成网站的意图
     * @param userPrompt 用户输入
     * @return JSON 结果 {"hasIntent": true/false, "reason": "说明"}
     */
    @SystemMessage(fromResource = "prompts/lowcode/intent-check-system-prompt.txt")
    String checkIntent(@UserMessage String userPrompt);

    /**
     * 代码校验：审查生成的代码找出问题
     * @param generatedCode 生成的代码 JSON
     * @return 问题列表，每行一个问题
     */
    @UserMessage(fromResource = "prompts/lowcode/validate-code-user-prompt.txt")
    String validateCode(String generatedCode);
}
