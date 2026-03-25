package com.mordan.aihub.studydemo.langgraph.state;

import dev.langchain4j.data.message.ChatMessage;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.Map;

/**
 * @className: ChatState
 * @description: chat state
 * @author: 91002183
 * @date: 2026/3/25
 **/
// ─────────────────────────────────────────────────────────
// 【第 3 章 §3.2】定义 State
//
// MessagesState<ChatMessage> 是 LangGraph4j 预置的状态类，
// 内部已定义 Schema：
//   "messages" -> AppenderChannel（追加式，新消息加到末尾）
//
// 等价于自己写：
//   Map.of("messages", Channels.appender(ArrayList::new))
//
// AppenderChannel 是 Reducer 的一种实现：
//   旧值 = [msg1, msg2]
//   新值 = msg3
//   归并结果 = [msg1, msg2, msg3]  （追加，不覆盖）
// ─────────────────────────────────────────────────────────
public class ChatState extends MessagesState<ChatMessage> {

    public ChatState(Map<String, Object> initData) {
        super(initData);
    }

}
