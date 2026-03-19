package com.mordan.aihub.fitness.config;

import com.mordan.aihub.fitness.ai.FitnessPlanAiService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 健身 AI 服务工厂
 * 复用项目已有的 ChatLanguageModel Bean
 *
 * @author fitness
 */
@Configuration
public class FitnessAiServiceFactory {

    @Resource
    private ChatModel chatModel;

    @Bean
    public FitnessPlanAiService fitnessPlanAiService() {
        return AiServices.builder(FitnessPlanAiService.class)
                .chatModel(chatModel)
                .build();
    }
}
