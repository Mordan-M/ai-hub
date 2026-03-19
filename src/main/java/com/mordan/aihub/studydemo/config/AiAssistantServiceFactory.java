package com.mordan.aihub.studydemo.config;

import com.mordan.aihub.studydemo.assistant.AiAssistantService;
import com.mordan.aihub.studydemo.tool.CalculateTool;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @className: AiCodeHelperServiceFactory
 * @description: ai 助手工厂
 * @author: 91002183
 * @date: 2026/3/11
 **/
@Configuration
public class AiAssistantServiceFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    private StreamingChatModel streamingChatModel;

    @Resource
    private ContentRetriever contentRetriever;

    @Resource
    private McpToolProvider mcpToolProvider;

    @Bean
    public AiAssistantService knowledgeAssistantService() {
        // 会话记忆
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);

        // 构造 AI Service
        return AiServices.builder(AiAssistantService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemory(chatMemory)
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.withMaxMessages(10)) // 每个会话独立存储
                .contentRetriever(contentRetriever) // RAG 检索增强生成
                .tools(new CalculateTool()) // 工具调用
                .toolProvider(mcpToolProvider) // MCP 工具调用
                .build();
    }

}
