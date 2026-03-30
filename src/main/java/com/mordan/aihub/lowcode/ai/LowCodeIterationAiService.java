package com.mordan.aihub.lowcode.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * @className: LowCodeIterationAiService
 * @description: 迭代对话 AI 服务接口
 * @author: 91002183
 * @date: 2026/3/30
 **/
public interface LowCodeIterationAiService {

    /**
     * 多轮对话：理解用户的修改意图，返回结构化修改指令
     * memoryId 使用 conversationId，保证同一对话上下文连续
     *
     * @param memoryId    conversationId，区分不同用户的对话
     * @param userMessage 用户的修改描述
     * @return 结构化修改指令 JSON
     */
    @SystemMessage(fromResource = "prompts/lowcode/iteration-chat-system-prompt.txt")
    String chat(@MemoryId String memoryId, @UserMessage String userMessage);

    /**
     * 生成 patch：根据修改指令 + 相关文件生成差量代码
     * 无需 memory，每次 patch 生成是独立的
     *
     * @param userPrompt 修改指令 + 相关文件内容
     * @return patch JSON {"files":[...]}
     */
    @SystemMessage(fromResource = "prompts/lowcode/iteration-patch-system-prompt.txt")
    String generatePatch(@UserMessage String userPrompt);

}
