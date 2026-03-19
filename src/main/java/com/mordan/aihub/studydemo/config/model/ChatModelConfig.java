//package com.mordan.ai.study.aistudy.config.model;
//
//import dev.langchain4j.model.chat.ChatModel;
//import dev.langchain4j.model.chat.listener.ChatModelListener;
//import dev.langchain4j.model.openai.OpenAiChatModel;
//import jakarta.annotation.Resource;
//import lombok.Data;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.util.List;
//
//@Configuration
//@ConfigurationProperties(prefix = "langchain4j.open-ai.chat-model")
//@Data
//public class ChatModelConfig {
//
//    private String modelName;
//
//    private String apiKey;
//
//    private double temperature;
//
//    private int maxTokens;
//
//    private boolean logRequests;
//
//    private boolean logResponses;
//
//    @Resource
//    private ChatModelListener chatModelListener;
//
//    @Bean
//    public ChatModel chatModel() {
//        return OpenAiChatModel.builder()
//                .apiKey(apiKey)
//                .modelName(modelName)
//                .temperature(temperature)
//                .maxTokens(maxTokens)
//                .logRequests(logRequests)
//                .logResponses(logResponses)
//                .listeners(List.of(chatModelListener))             // 注入 listener)
//                .build();
//    }
//}
