package com.mordan.aihub.lowcode.config;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mordan.aihub.lowcode.ai.LowCodeGenerateAiService;
import com.mordan.aihub.lowcode.ai.LowCodeIterationAiService;
import com.mordan.aihub.studydemo.tool.CalculateTool;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 低代码 AI 服务配置
 * 整合所有低代码生成功能到单个服务
 */
@Configuration
@Slf4j
public class LowCodeAiServiceFactory {

    @Resource(name = "aiHubChatModel")
    private OpenAiChatModel lowCodeChatModel;

    @Resource
    private StreamingChatModel streamingChatModel;

    @Bean
    public LowCodeGenerateAiService lowCodeGenerateAiService() {
        // 会话记忆
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(20);

        return AiServices.builder(LowCodeGenerateAiService.class)
                .chatModel(lowCodeChatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemory(chatMemory)
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.withMaxMessages(20)) // 每个会话独立存储
                .tools(new CalculateTool()) // 工具调用
                .build();
    }

    /**
     * 有状态迭代对话服务
     * - chat() 方法按 conversationId 维护独立 memory
     * - generatePatch() 方法无状态，不受 memory 影响
     */
    @Bean
    public LowCodeIterationAiService lowCodeIterationAiService() {

        // Guava Cache：最多 500 个会话，30 分钟无活动自动清除
        Cache<String, ChatMemory> memoryCache = CacheBuilder.newBuilder()
                .maximumSize(500)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .removalListener(notification ->
                        log.info("ChatMemory evicted for conversationId={}", notification.getKey()))
                .build();

        return AiServices.builder(LowCodeIterationAiService.class)
                .chatModel(lowCodeChatModel)
                .chatMemoryProvider(memoryId -> {
                    String id = memoryId.toString();
                    ChatMemory memory = memoryCache.getIfPresent(id);
                    if (memory == null) {
                        // 保留最近 20 条消息，够用且不超 context window
                        memory = MessageWindowChatMemory.withMaxMessages(20);
                        memoryCache.put(id, memory);
                    }
                    return memory;
                })
                .build();
    }
}
