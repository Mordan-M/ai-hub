package com.mordan.aihub.langgraph4j;

import com.mordan.aihub.studydemo.langgraph.AdvancedAgentWorkFlow;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
class AdvancedAgentWorkFlowTest {

    @Resource
    private AdvancedAgentWorkFlow advancedAgentWorkFlow;

    @Test
    void testAdvancedWorkflow() throws Exception {
        advancedAgentWorkFlow.executeWorkflow();
    }
}
