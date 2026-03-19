package com.mordan.aihub.studydemo.assistant.impl;

import com.mordan.aihub.studydemo.assistant.AiWorkFlowService;
import dev.langchain4j.agentic.UntypedAgent;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @className: AiWorkFlowServiceImpl
 * @description: TODO 类描述
 * @author: 91002183
 * @date: 2026/3/16
 **/
@Service
public class AiWorkFlowServiceImpl implements AiWorkFlowService {


    // 注入已装配好的工作流，直接调用即可
    @Resource
    private UntypedAgent novelCreatorWorkflow;

    @Override
    public String storyWorkFlow(String topic, String audience, String style) {
        Map<String, Object> input = Map.of(
                "topic",    topic,
                "audience", audience,
                "style",    style
        );
        return (String) novelCreatorWorkflow.invoke(input);
    }

}
