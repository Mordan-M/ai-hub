package com.mordan.aihub.studydemo.assistant.controller;

import com.mordan.aihub.studydemo.assistant.AiAssistantService;
import com.mordan.aihub.studydemo.assistant.AiWorkFlowService;
import jakarta.annotation.Resource;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;


/**
 * @className: AssistantController
 * @description: ai 助手控制类
 * @author: 91002183
 * @date: 2026/3/6
 **/
@RestController
@RequestMapping("/ai-hub/demo/assistant")
public class AiAssistantController {

    @Resource
    private AiAssistantService knowledgeAssistantService;

    @Resource
    private AiWorkFlowService aiWorkFlowService;

    @GetMapping("/chat/direct")
    public String chat(String message) {
        return knowledgeAssistantService.chat(message);
    }

    @GetMapping("/chat")
    public Flux<ServerSentEvent<String>> chat(int memoryId, String message) {
        return knowledgeAssistantService.chatStream(memoryId, message)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }

    @GetMapping("/story/work/flow")
    public String workFlow(String topic, String audience, String style) {
        return aiWorkFlowService.storyWorkFlow(topic, audience, style);
    }

}
