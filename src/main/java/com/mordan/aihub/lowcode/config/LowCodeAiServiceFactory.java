package com.mordan.aihub.lowcode.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mordan.aihub.lowcode.ai.IntentCheckAiService;
import com.mordan.aihub.lowcode.ai.LowCodeGenerateAiService;
import com.mordan.aihub.lowcode.ai.ParseIntentAiService;
import com.mordan.aihub.lowcode.ai.RepairCodeAiService;
import com.mordan.aihub.lowcode.ai.ValidateAiService;
import com.mordan.aihub.lowcode.memory.PersistentChatMemoryStore;
import com.mordan.aihub.lowcode.tools.ToolManager;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 低代码 AI 服务配置
 * 拆分：简单服务（无工具）+ 生成服务（带工具）
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

    @Resource
    private ObjectMapper objectMapper;

    @Bean
    public Cache<String, ChatMemory> sharedMemoryCache() {
        return CacheBuilder.newBuilder()
                .maximumSize(500)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .removalListener(notification ->
                        log.info("ChatMemory evicted for conversationId={}", notification.getKey()))
                .build();
    }

    /**
     * 意图检查AI服务
     */
    @Bean
    public IntentCheckAiService intentCheckAiService(PersistentChatMemoryStore chatMemoryStore,
                                                       Cache<String, ChatMemory> sharedMemoryCache) throws IOException {
        return AiServices.builder(IntentCheckAiService.class)
                .chatModel(lowCodeChatModel)
                .chatMemoryProvider(memoryId -> this.buildChatMemory(memoryId, chatMemoryStore, sharedMemoryCache))
                .build();
    }

    /**
     * 意图解析AI服务
     */
    @Bean
    public ParseIntentAiService parseIntentAiService(PersistentChatMemoryStore chatMemoryStore,
                                                      Cache<String, ChatMemory> sharedMemoryCache) {
        return AiServices.builder(ParseIntentAiService.class)
                .chatModel(lowCodeChatModel)
                .chatMemoryProvider(memoryId -> this.buildChatMemory(memoryId, chatMemoryStore, sharedMemoryCache))
                .build();
    }

    /**
     * 修复代码 AI 服务
     * @param chatMemoryStore 入参
     * @param sharedMemoryCache 入参
     * @return 执行结果
     */
    @Bean
    public RepairCodeAiService repairCodeAiService(PersistentChatMemoryStore chatMemoryStore,
                                                   Cache<String, ChatMemory> sharedMemoryCache) {
        return AiServices.builder(RepairCodeAiService.class)
                .chatModel(lowCodeChatModel)
                .chatMemoryProvider(memoryId -> this.buildChatMemory(memoryId, chatMemoryStore, sharedMemoryCache))
                .build();
    }


    /**
     * 代码生成 AI 服务（带文件工具）
     * 仅用于代码生成阶段，需要文件操作工具进行迭代开发
     */
    @Bean
    public LowCodeGenerateAiService lowCodeGenerateAiService(PersistentChatMemoryStore chatMemoryStore,
                                                             Cache<String, ChatMemory> sharedMemoryCache) {
        return AiServices.builder(LowCodeGenerateAiService.class)
                .chatModel(lowCodeChatModel)
                .chatMemoryProvider(memoryId -> this.buildChatMemory(memoryId, chatMemoryStore, sharedMemoryCache))
                .tools(toolManager.getAllTools())
                // 处理工具调用幻觉问题
                .hallucinatedToolNameStrategy(toolExecutionRequest ->
                        ToolExecutionResultMessage.from(toolExecutionRequest,
                                "Error: there is no tool called " + toolExecutionRequest.name())
                )
                .maxSequentialToolsInvocations(20)  // 最多连续调用 20 次工具
                .build();
    }

    private void normalizeToolArguments(ToolExecutionRequest request) {
        String arguments = request.arguments();
        if (!"writeFile".equals(request.name()) && !"modifyFile".equals(request.name())) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(arguments);
            if (root instanceof ObjectNode objectNode && root.has("content")) {
                JsonNode contentNode = root.get("content");
                if (!contentNode.isTextual()) {
                    String contentStr = objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(contentNode);
                    objectNode.put("content", contentStr);
                }
            }

        } catch (Exception e) {
            log.warn("normalizeToolArguments failed, use original. tool={}", request.name(), e);
        }
    }

    /**
     * 无状态 AI 服务
     *
     * @return 执行结果
     */
    @Bean
    public ValidateAiService validateAiService() {
        return AiServices.builder(ValidateAiService.class)
                .chatModel(lowCodeChatModel)
                .build();
    }

    private ChatMemory buildChatMemory(Object memoryId,
                                       PersistentChatMemoryStore chatMemoryStore,
                                       Cache<String, ChatMemory> sharedMemoryCache) {
        return MessageWindowChatMemory.withMaxMessages(10);
//
//        String id = String.valueOf(memoryId);
//        ChatMemory memory = sharedMemoryCache.getIfPresent(id);
//        if (memory == null) {
//            memory = MessageWindowChatMemory.builder()
//                    .id(memoryId)
//                    .maxMessages(50)
//                    .chatMemoryStore(chatMemoryStore) // 关键：绑定持久化 store
//                    .build();
//            sharedMemoryCache.put(id, memory);
//        }
//        return memory;
    }

}
