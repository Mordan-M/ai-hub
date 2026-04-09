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
import java.util.Objects;

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
    private Integer maxTokens;
    private boolean logRequests;
    private boolean logResponses;
    private int connectTimeout;
    private int readTimeout;
    private Integer maxCompletionTokens;

    @Resource
    private ChatModelListener chatModelListener;

    @Bean(name = "aiHubChatModel")
    @Primary
    public OpenAiChatModel aiHubChatModel() {
        // langchain4j 中 timeout 同时设置 connect 和 read timeout
        // 我们设置为最大的 readTimeout 给两者，因为 connect 很快
        Duration timeout = Duration.ofMillis(readTimeout);

        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .timeout(timeout);
        // 兼容 open ai 新模型
        if (Objects.nonNull(maxCompletionTokens)) {
            builder.maxCompletionTokens(maxCompletionTokens);
        } else {
            builder.maxTokens(maxTokens);
        }

        return builder.build();
    }
}
