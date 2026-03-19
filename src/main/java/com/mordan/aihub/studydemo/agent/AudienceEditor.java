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
public interface AudienceEditor {

    @UserMessage("""
                你是一位专业编辑。
                将以下故事改写，使其更适合目标受众：{{audience}}。
                只返回改写后的故事，不要其他文字。
                故事内容："{{story}}"
                """)
    @Agent(value = "将故事改写以适合特定受众", outputKey = "story")
    String editForAudience(@V("story") String story, @V("audience") String audience);

}
