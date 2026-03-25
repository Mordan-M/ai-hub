package com.mordan.aihub.studydemo.langgraph.app;

import com.mordan.aihub.studydemo.langgraph.node.GreeterNode;
import com.mordan.aihub.studydemo.langgraph.node.ResponderNode;
import com.mordan.aihub.studydemo.langgraph.state.SimpleState;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;

import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * @className: SimpleGraphApp
 * @description: simple graph app
 * @author: 91002183
 * @date: 2026/3/24
 **/
public class SimpleGraphApp {
    public static void main(String[] args) throws GraphStateException {
        // Initialize nodes
        GreeterNode greeterNode = new GreeterNode();
        ResponderNode responderNode = new ResponderNode();

        // Define the graph structure
        var stateGraph = new StateGraph<>(SimpleState.SCHEMA, initData -> new SimpleState(initData))
                .addNode("greeter", node_async(greeterNode))
                .addNode("responder", node_async(responderNode))
                // Define edges
                .addEdge(START, "greeter") // Start with the greeter node
                .addEdge("greeter", "responder")
                .addEdge("responder", END)   // End after the responder node
                ;
        // Compile the graph
        var compiledGraph = stateGraph.compile();

        // Run the graph
        // The `stream` method returns an AsyncGenerator.
        // For simplicity, we'll collect results. In a real app, you might process them as they arrive.
        // Here, the final state after execution is the item of interest.

        for (var item : compiledGraph.stream( Map.of( SimpleState.MESSAGES_KEY, "Let's, begin!" ) ) ) {
            System.out.println( item );
        }

    }
}
