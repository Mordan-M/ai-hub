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
public interface StyleEditor {

    @UserMessage("""
                你是一位专业编辑。
                将以下故事改写，使其更符合 {{style}} 风格。
                只返回改写后的故事，不要其他文字。
                故事内容："{{story}}"
                """)
    @Agent(value = "将故事改写为特定风格", outputKey = "story")
    String editStyle(@V("story") String story, @V("style") String style);

}
