package com.mordan.aihub.studydemo.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

/**
 * @className: KnowledgeAssistant
 * @description: ai 小助手
 * @author: 91002183
 * @date: 2026/3/11
 **/
public interface AiAssistantService {

    @SystemMessage("You are a polite assistant")
    String chat(String userMessage);

    // 流式对话
    @SystemMessage(fromResource = "prompts/knowledge-assistant-prompt.txt")
    Flux<String> chatStream(@MemoryId int memoryId, @UserMessage String userMessage);

}
