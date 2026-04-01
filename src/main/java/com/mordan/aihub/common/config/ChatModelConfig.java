package com.mordan.aihub.common.config;

import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * 低代码专用 ChatModel 配置
 * 代码生成需要更长的超时时间，单独配置
 */
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.chat-model")
@Data
public class ChatModelConfig {

    private String baseUrl;
    private String apiKey;
    private String modelName;
    private double temperature;
    private int maxTokens;
    private boolean logRequests;
    private boolean logResponses;
    private int connectTimeout;
    private int readTimeout;

    @Resource
    private ChatModelListener chatModelListener;

    @Bean(name = "aiHubChatModel")
    @Primary
    public OpenAiChatModel aiHubChatModel() {
        // langchain4j 中 timeout 同时设置 connect 和 read timeout
        // 我们设置为最大的 readTimeout 给两者，因为 connect 很快
        Duration timeout = Duration.ofMillis(readTimeout);

        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .timeout(timeout)
//                .listeners(List.of(chatModelListener))
                .build();
    }
}
