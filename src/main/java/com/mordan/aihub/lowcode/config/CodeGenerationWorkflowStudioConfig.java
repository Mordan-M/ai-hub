package com.mordan.aihub.lowcode.config;

import com.mordan.aihub.lowcode.workflow.CodeGenerationWorkflow;
import jakarta.annotation.Resource;
import org.bsc.langgraph4j.studio.LangGraphStudioServer;
import org.bsc.langgraph4j.studio.springboot.LangGraphStudioConfig;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * 代码生成工作流可视化调试配置
 * 用于 LangGraph Studio 可视化调试 (langgraph4j 1.8.10)
 */
@Configuration
public class CodeGenerationWorkflowStudioConfig extends LangGraphStudioConfig {

    @Resource
    private CodeGenerationWorkflow codeGenerationWorkflow;

    /**
     * 暴露 StateGraph 给 LangGraph Studio 进行可视化
     * langgraph4j 1.8.10 studio 会自动检测注册 StateGraph bean
     */
    @Override
    public Map<String, LangGraphStudioServer.Instance> instanceMap() {
        var instance = LangGraphStudioServer.Instance.builder()
                .title("My AI Agent")
                .graph(codeGenerationWorkflow.compiledGraph().stateGraph)
                .build();
        return Map.of("default", instance);
    }

}
