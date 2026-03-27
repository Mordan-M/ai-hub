package com.mordan.aihub.lowcode.workflow.state;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.ArrayList;
import java.util.Map;

/**
 * 代码生成工作流状态
 * 将完整上下文封装在一个对象中，便于维护
 */
public class WorkflowState extends AgentState {

    /**
     * 上下文对象在状态中的 Key
     */
    public static final String CONTEXT_KEY = "context";

    public WorkflowState(Map<String, Object> initData) {
        super(initData);
    }

    /**
     * 定义状态Schema配置
     */
    public static Map<String, Channel<?>> schema() {
        return Map.of(
                // validationErrors 需要使用 appender 模式累积错误
                "validationErrors", Channels.appender(ArrayList<String>::new)
        );
    }

    /**
     * 获取完整上下文
     */
    public GenerationWorkflowContext context() {
        return this.<GenerationWorkflowContext>value(CONTEXT_KEY).orElse(null);
    }

    /**
     * 保存上下文到状态
     */
    public static Map<String, Object> saveContext(GenerationWorkflowContext context) {
        return Map.of(CONTEXT_KEY, context);
    }
}
