package com.mordan.aihub.langgraph4j;

import com.mordan.aihub.studydemo.langgraph.StatefulChatWorkFlow;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
class StatefulChatWorkFlowTest {

    @Resource
    private StatefulChatWorkFlow statefulChatWorkFlow;

    @Test
    public void testStatefulChatWorkFlow() throws Exception {
        statefulChatWorkFlow.executeWorkflow();
    }

}
