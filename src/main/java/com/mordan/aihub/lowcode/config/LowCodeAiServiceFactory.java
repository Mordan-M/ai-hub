package com.mordan.aihub.lowcode.config;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mordan.aihub.lowcode.ai.LowCodeGenerateAiService;
import com.mordan.aihub.lowcode.ai.LowCodeStatelessAiService;
import com.mordan.aihub.lowcode.tools.ToolManager;
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

    @Resource
    private ToolManager toolManager;

    @Bean
    public LowCodeGenerateAiService lowCodeGenerateAiService(com.mordan.aihub.lowcode.ai.memory.PersistentChatMemoryStore chatMemoryStore) {
        // Guava Cache：最多 500 个会话，30 分钟无活动自动清除
        Cache<String, ChatMemory> memoryCache = CacheBuilder.newBuilder()
                .maximumSize(500)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .removalListener(notification ->
                        log.info("ChatMemory evicted for conversationId={}", notification.getKey()))
                .build();

        return AiServices.builder(LowCodeGenerateAiService.class)
                .chatModel(lowCodeChatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(memoryId -> {
                    String id = memoryId.toString();
                    ChatMemory memory = memoryCache.getIfPresent(id);
                    if (memory == null) {
                        memory = MessageWindowChatMemory.builder()
                                .id(id)
                                .maxMessages(20)
                                .chatMemoryStore(chatMemoryStore) // 关键：绑定持久化 store
                                .build();
                        memoryCache.put(id, memory);
                    }
                    return memory;
                })
                .tools(toolManager.getAllTools())
                .build();
    }

    /**
     * 无状态 AI 服务
     * @return 执行结果
     */
    @Bean
    public LowCodeStatelessAiService lowCodeStatelessAiService() {
        return AiServices.builder(LowCodeStatelessAiService.class)
                .chatModel(lowCodeChatModel)
                .streamingChatModel(streamingChatModel)
                .build();
    }

}
