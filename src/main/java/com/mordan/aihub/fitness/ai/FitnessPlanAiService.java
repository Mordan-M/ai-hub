package com.mordan.aihub.fitness.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * AI 健身计划生成服务
 * 复用项目已有的 ChatLanguageModel Bean
 * 系统提示词从 classpath 资源文件读取
 *
 * @author fitness
 */
public interface FitnessPlanAiService {

    @SystemMessage(fromResource = "prompts/fitness-plan-system-prompt.txt")
    String generateWeeklyPlan(@UserMessage String userPrompt);
}
