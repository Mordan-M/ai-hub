package com.mordan.aihub.studydemo.langgraph.node;

import com.mordan.aihub.studydemo.langgraph.state.SimpleState;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.Map;

/**
 * @className: GreeterNode
 * @description: greeter node
 * @author: 91002183
 * @date: 2026/3/24
 **/

// Node that adds a greeting
public class GreeterNode implements NodeAction<SimpleState> {
    @Override
    public Map<String, Object> apply(SimpleState state) {
        System.out.println("GreeterNode executing. Current messages: " + state.messages());
        return Map.of(SimpleState.MESSAGES_KEY, "Hello from GreeterNode!");
    }
}