package com.mordan.aihub.studydemo.config.agent;

import com.mordan.aihub.studydemo.agent.AudienceEditor;
import com.mordan.aihub.studydemo.agent.CreativeWriter;
import com.mordan.aihub.studydemo.agent.StyleEditor;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @className: AgentConfig
 * @description: TODO 类描述
 * @author: 91002183
 * @date: 2026/3/16
 **/
@Configuration


public class AgentConfig {

    //  1. 三个 Agent Bean，各自注入同一个 ChatModel
    @Bean
    public CreativeWriter creativeWriter(ChatModel chatModel) {
        return AgenticServices.agentBuilder(CreativeWriter.class)
                .chatModel(chatModel)
                .outputKey("story")
                .build();
    }

    @Bean
    public AudienceEditor audienceEditor(ChatModel chatModel) {
        return AgenticServices.agentBuilder(AudienceEditor.class)
                .chatModel(chatModel)
                .outputKey("story")
                .build();
    }

    @Bean
    public StyleEditor styleEditor(ChatModel chatModel) {
        return AgenticServices.agentBuilder(StyleEditor.class)
                .chatModel(chatModel)
                .outputKey("story")
                .build();
    }

    // 2. 工作流 Bean，依赖上面三个 Agent Bean（Spring 自动按类型注入）
    @Bean
    public UntypedAgent novelCreatorWorkflow(
            CreativeWriter creativeWriter,
            AudienceEditor audienceEditor,
            StyleEditor styleEditor) {
        return AgenticServices.sequenceBuilder()
                .subAgents(creativeWriter, audienceEditor, styleEditor)
                .outputKey("story")
                .build();
    }

}
