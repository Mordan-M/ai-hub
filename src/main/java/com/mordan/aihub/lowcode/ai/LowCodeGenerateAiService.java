package com.mordan.aihub.lowcode.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 低代码生成 AI 服务
 * 仅包含需要文件操作工具的代码生成方法
 */
public interface LowCodeGenerateAiService {

    /**
     * 代码生成：根据解析后的需求生成完整 Vue 项目
     * @param appId 应用ID（用于记忆）
     * @param userPrompt 结构化需求
     * @return JSON 结果 {"files":[{"path":"...","content":"..."}]}
     */
    @SystemMessage(fromResource = "prompts/lowcode/generate-code-system-prompt.txt")
    String generateCode(@MemoryId String appId, @UserMessage String userPrompt);

}
