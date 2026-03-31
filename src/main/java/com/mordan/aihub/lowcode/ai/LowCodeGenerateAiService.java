package com.mordan.aihub.lowcode.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 低代码生成 AI 服务
 * 整合意图检查、意图解析、代码生成、代码校验、代码修复所有功能
 */
public interface LowCodeGenerateAiService {

    /**
     * 意图解析：将自然语言需求解析为结构化 JSON
     * @param userPrompt 用户输入
     * @return 结构化 JSON
     */
    @SystemMessage(fromResource = "prompts/lowcode/parse-intent-system-prompt.txt")
    String parseIntent(@MemoryId String appId, @UserMessage String userPrompt);

    /**
     * 代码生成：根据解析后的需求生成完整 Vue 项目
     * @param userPrompt 结构化需求
     * @return JSON 结果 {"files":[{"path":"...","content":"..."}]}
     */
    @SystemMessage(fromResource = "prompts/lowcode/generate-code-system-prompt.txt")
    String generateCode(@MemoryId String appId, @UserMessage String userPrompt);


    /**
     * 代码修复：根据错误列表修复代码
     * @param userPrompt 错误列表+优化建议+原始代码
     * @return JSON 结果 {"files":[{"path":"...","content":"..."}]}
     */
    @SystemMessage(fromResource = "prompts/lowcode/repair-code-system-prompt.txt")
    String repairCode(@MemoryId String appId, @UserMessage String userPrompt);

}
