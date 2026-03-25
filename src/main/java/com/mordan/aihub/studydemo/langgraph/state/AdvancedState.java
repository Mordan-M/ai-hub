package com.mordan.aihub.studydemo.langgraph.state;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AdvancedState —— 进阶 Demo 的共享状态
 *
 * 字段说明：
 *   topic           用户输入的新闻话题（默认覆盖）
 *   valid           router 节点校验结果（默认覆盖）
 *   analysisResults 并行分析节点的输出列表（AppenderChannel 追加）
 *   summary         最终摘要文本（默认覆盖）
 *
 * Schema 设计重点：
 *   - analysisResults 使用 AppenderChannel，sentimentAnalyzer 和
 *     keywordExtractor 并行写入时各自追加，不会互相覆盖。
 *   - 其余字段无需 Channel，新值直接覆盖旧值即可。
 */
public class AdvancedState extends AgentState {

    public static final Map<String, Channel<?>> SCHEMA = Map.of(
            // AppenderChannel：每次返回的新值追加到列表末尾
            // 这是并行节点能安全写入同一字段的关键
            "analysisResults", Channels.appender(ArrayList::new)
            // topic / valid / summary 不配置 Channel，
            // 默认行为是新值覆盖旧值
    );

    public AdvancedState(Map<String, Object> initData) {
        super(initData);
    }

    /** 用户输入的话题 */
    public String topic() {
        return this.<String>value("topic").orElse("");
    }

    /** router 节点的校验结果 */
    public boolean valid() {
        return this.<Boolean>value("valid").orElse(false);
    }

    /**
     * 并行分析结果列表
     * sentimentAnalyzer 和 keywordExtractor 各追加一条
     */
    public List<String> analysisResults() {
        return this.<List<String>>value("analysisResults").orElse(List.of());
    }

    /** 最终摘要（由子图的 polish 节点写入） */
    public String summary() {
        return this.<String>value("summary").orElse("");
    }
}
