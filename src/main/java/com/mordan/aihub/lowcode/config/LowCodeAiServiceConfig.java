package com.mordan.aihub.lowcode.config;

import com.mordan.aihub.lowcode.ai.GenerateCodeAiService;
import com.mordan.aihub.lowcode.ai.IntentCheckAiService;
import com.mordan.aihub.lowcode.ai.ParseIntentAiService;
import com.mordan.aihub.lowcode.ai.RepairCodeAiService;
import com.mordan.aihub.lowcode.ai.ValidateCodeAiService;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 低代码 AI 服务配置
 * 将各个 AI 服务接口创建为 Spring Bean
 */
@Configuration
public class LowCodeAiServiceConfig {

    @Resource(name = "aiHubChatModel")
    private OpenAiChatModel lowCodeChatModel;

    @Bean
    public IntentCheckAiService intentCheckAiService() {
        return AiServices.builder(IntentCheckAiService.class)
                .chatModel(lowCodeChatModel)
                .build();
    }

    @Bean
    public ParseIntentAiService parseIntentAiService() {
        return AiServices.builder(ParseIntentAiService.class)
                .chatModel(lowCodeChatModel)
                .build();
    }

    @Bean
    public GenerateCodeAiService generateCodeAiService() {
        return AiServices.builder(GenerateCodeAiService.class)
                .chatModel(lowCodeChatModel)
                .build();
    }

    @Bean
    public ValidateCodeAiService validateCodeAiService() {
        return AiServices.builder(ValidateCodeAiService.class)
                .chatModel(lowCodeChatModel)
                .build();
    }

    @Bean
    public RepairCodeAiService repairCodeAiService() {
        return AiServices.builder(RepairCodeAiService.class)
                .chatModel(lowCodeChatModel)
                .build();
    }
}
