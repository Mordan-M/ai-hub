package com.mordan.aihub.studydemo.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * @className: CreativeWriter
 * @description: TODO 类描述
 * @author: 91002183
 * @date: 2026/3/16
 **/
public interface CreativeWriter {

    @UserMessage("""
                你是一位创意写手。
                围绕给定主题生成一篇不超过 3 句话的故事草稿。
                只返回故事内容，不要其他文字。
                主题是：{{topic}}
                """)
    @Agent(value = "根据主题生成故事草稿", outputKey = "story")
    String generateStory(@V("topic") String topic);

}
