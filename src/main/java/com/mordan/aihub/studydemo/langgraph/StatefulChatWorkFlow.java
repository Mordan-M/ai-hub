package com.mordan.aihub.studydemo.langgraph;

/**
 * ============================================================
 *  LangGraph4j 基础阶段 Demo：有状态多步对话
 * ============================================================
 *
 * 本 Demo 完整覆盖第 3-4 章核心知识点：
 *
 *  ┌─────────────────────────────────────────────────────────┐
 *  │  第 3 章：核心概念                                        │
 *  │  ✅ State / Schema / Reducer（AppenderChannel）          │
 *  │  ✅ Node（同步 & 携带 RunnableConfig 的节点）             │
 *  │  ✅ Edge（普通边 & 条件边）                               │
 *  │  ✅ StateGraph 编译（CompileConfig）                      │
 *  │  ✅ 流式执行（.stream()）与同步执行（.execute()）          │
 *  │                                                         │
 *  │  第 4 章：持久化与断点                                    │
 *  │  ✅ MemorySaver（Checkpointer）                          │
 *  │  ✅ threadId 多会话隔离                                   │
 *  │  ✅ getState() / getStateHistory()                       │
 *  │  ✅ updateState() 手动修改状态                            │
 *  └─────────────────────────────────────────────────────────┘
 *
 * 场景说明：
 *   一个带有"天气查询"工具的多轮对话 Agent。
 *   用户可以进行多轮对话，Agent 会记住之前的消息内容。
 *   不同 threadId 之间的会话完全隔离，互不干扰。
 *
 * Maven 依赖：
 * <dependency>
 *     <groupId>org.bsc.langgraph4j</groupId>
 *     <artifactId>langgraph4j-core</artifactId>
 *     <version>1.8.10</version>
 * </dependency>
 * <dependency>
 *     <groupId>org.bsc.langgraph4j</groupId>
 *     <artifactId>langgraph4j-langchain4j</artifactId>
 *     <version>1.8.10</version>
 * </dependency>
 * <dependency>
 *     <groupId>dev.langchain4j</groupId>
 *     <artifactId>langchain4j-open-ai</artifactId>
 *     <version>1.9.1</version>
 * </dependency>
 */

import com.mordan.aihub.studydemo.langgraph.state.ChatState;
import com.mordan.aihub.studydemo.tool.WeatherTool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.EdgeAction;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.bsc.langgraph4j.langchain4j.serializer.std.ChatMesssageSerializer;
import org.bsc.langgraph4j.langchain4j.serializer.std.ToolExecutionRequestSerializer;
import org.bsc.langgraph4j.serializer.std.ObjectStreamStateSerializer;
import org.springframework.stereotype.Component;

import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Component
@Slf4j
public class StatefulChatWorkFlow {

    @Resource
    private ChatModel chatModel;

    public void executeWorkflow() throws Exception {

        // ─────────────────────────────────────────────────────
        // 【第 3 章 §3.2】配置序列化器
        //
        // Checkpoint 持久化状态时需要序列化 State 中的对象。
        // LangChain4j 的 ChatMessage、ToolExecutionRequest
        // 不是标准 Java Serializable，需要注册自定义序列化器。
        // ─────────────────────────────────────────────────────
        var stateSerializer = new ObjectStreamStateSerializer<>(ChatState::new);
        stateSerializer.mapper()
                .register(ToolExecutionRequest.class, new ToolExecutionRequestSerializer())
                .register(ChatMessage.class, new ChatMesssageSerializer());

        // ─────────────────────────────────────────────────────
        // 配置 LLM 模型
        // ─────────────────────────────────────────────────────
        var weatherTool = new WeatherTool();
        var toolSpecs = ToolSpecifications.toolSpecificationsFrom(WeatherTool.class);

        // ─────────────────────────────────────────────────────
        // 【第 3 章 §3.3】定义 Node（节点）
        //
        // 节点是 AsyncNodeAction<S> 的实现：
        //   - 输入：当前 State
        //   - 输出：Map<String, Object>（状态更新，由 Reducer 归并）
        //
        // node_async() 将同步 NodeAction 包装为异步版本。
        // ─────────────────────────────────────────────────────

        // ── 节点 1：callAgent —— 调用 LLM ──
        var callAgent = node_async((ChatState state) -> {
            System.out.println("\n[callAgent] 当前消息数：" + state.messages().size());

            var request = ChatRequest.builder()
                    .messages(state.messages())
                    .toolSpecifications(toolSpecs)   // 告诉 LLM 可以调用哪些工具
                    .build();

            var response = chatModel.chat(request);
            var aiMsg = response.aiMessage();

            System.out.println("[callAgent] LLM 回复：" +
                    (aiMsg.hasToolExecutionRequests()
                            ? "请求调用工具 -> " + aiMsg.toolExecutionRequests().get(0).name()
                            : aiMsg.text()));

            // 返回状态更新：新消息会通过 AppenderChannel 追加到 messages 列表末尾
            return Map.of("messages", aiMsg);
        });

        // ── 节点 2：invokeTool —— 执行工具调用 ──
        var invokeTool = node_async((ChatState state) -> {
            // 取出 LLM 请求的工具调用指令
            var aiMsg = (AiMessage) state.lastMessage().orElse(null);
            var request = aiMsg.toolExecutionRequests().get(0);

            System.out.println("[invokeTool] 执行工具：" + request.name()
                    + "，参数：" + request.arguments());

            // 用 DefaultToolExecutor 反射调用 WeatherTool 的方法
            var executor = new DefaultToolExecutor(weatherTool, request);
            var result = executor.execute(request, null);

            System.out.println("[invokeTool] 工具结果：" + result);

            // 返回工具执行结果消息，追加到 messages 列表
            return Map.of("messages", AiMessage.from(result));
        });

        // ─────────────────────────────────────────────────────
        // 【第 3 章 §3.4】定义 Edge（边）
        //
        // 条件边（EdgeAction）：根据当前 State 返回目标节点名称。
        // edge_async() 将同步 EdgeAction 包装为异步版本。
        // ─────────────────────────────────────────────────────
        EdgeAction<ChatState> routeAfterAgent = state -> {
            var last = state.lastMessage().orElse( null);
            if (last instanceof AiMessage aiMsg && aiMsg.hasToolExecutionRequests()) {
                return "call_tool";   // LLM 请求工具调用 → 跳转到 invokeTool 节点
            }
            return "end";             // 直接回复 → 结束
        };

        // ─────────────────────────────────────────────────────
        // 【第 3 章 §3.1】构建 StateGraph 并编译
        //
        // 图结构：
        //
        //   START
        //     │
        //     ▼
        //  [callAgent]  ──── LLM 直接回复 ──────────────► END
        //     │
        //     │ LLM 请求工具调用
        //     ▼
        //  [invokeTool]
        //     │
        //     └─────────────────────────────────────────► [callAgent]（循环）
        // ─────────────────────────────────────────────────────
        var workflow = new StateGraph<>(ChatState.SCHEMA, stateSerializer)
                // 添加节点
                .addNode("callAgent", callAgent)
                .addNode("invokeTool", invokeTool)
                // 普通边：START → callAgent（固定入口）
                .addEdge(START, "callAgent")
                // 条件边：callAgent 执行后，根据路由函数决定下一步
                .addConditionalEdges(
                        "callAgent",
                        edge_async(routeAfterAgent),
                        Map.of(
                                "call_tool", "invokeTool",  // 路由函数返回 "call_tool" → invokeTool 节点
                                "end", END                   // 路由函数返回 "end" → 结束
                        )
                )
                // 普通边：invokeTool 执行完毕后，回到 callAgent（循环执行）
                .addEdge("invokeTool", "callAgent");

        // ─────────────────────────────────────────────────────
        // 【第 4 章 §4.1】配置 Checkpointer（内存存储）
        //
        // MemorySaver 将每步执行后的 State 保存在内存中。
        // 生产环境可换为 Postgres Checkpointer 持久化到数据库。
        // ─────────────────────────────────────────────────────
        var memorySaver = new MemorySaver();

        var compileConfig = CompileConfig.builder()
                .checkpointSaver(memorySaver)
                // recursionLimit：防止无限循环，默认 25
                .recursionLimit(10)
                .build();

        var graph = workflow.compile(compileConfig);

        // 显示工作流图
        var showGraph = graph.getGraph(GraphRepresentation.Type.MERMAID);
        log.info("工作流图:\n{}", showGraph.content());

        // ═════════════════════════════════════════════════════
        // 场景一：有状态多轮对话（同一个 threadId）
        // ═════════════════════════════════════════════════════
        System.out.println("\n" + "=".repeat(60));
        System.out.println("  场景一：有状态多轮对话（threadId = user-001）");
        System.out.println("=".repeat(60));

        // ─────────────────────────────────────────────────────
        // 【第 4 章 §4.2】RunnableConfig —— 通过 threadId 隔离会话
        //
        // 同一个 threadId 的多次调用会共享 Checkpoint 历史，
        // 图会自动加载上一次的 State，实现跨轮次记忆。
        // ─────────────────────────────────────────────────────
        var configUser1 = RunnableConfig.builder()
                .threadId("user-001")    // 用户 001 的会话
                .build();

        // ── 第 1 轮：自我介绍 ──
        System.out.println("\n>>> 第 1 轮对话：自我介绍");
        runTurn(graph, configUser1, "你好！我叫小明，很高兴认识你。");

        // ── 第 2 轮：测试记忆 ──
        System.out.println("\n>>> 第 2 轮对话：测试记忆");
        runTurn(graph, configUser1, "你还记得我叫什么名字吗？");

        // ── 第 3 轮：工具调用（触发天气查询） ──
        System.out.println("\n>>> 第 3 轮对话：天气查询（触发工具调用）");
        runTurn(graph, configUser1, "帮我查一下北京今天的天气");

        // ─────────────────────────────────────────────────────
        // 【第 4 章 §4.3】getState() —— 查看当前会话状态
        // ─────────────────────────────────────────────────────
        System.out.println("\n" + "-".repeat(60));
        System.out.println("  查看 user-001 的当前状态（getState）");
        System.out.println("-".repeat(60));

        var currentState = graph.getState(configUser1);
        var messages = currentState.state().messages();
        System.out.println("消息总数：" + messages.size());
        System.out.println("最后一条消息：" + messages.get(messages.size() - 1));

        // ─────────────────────────────────────────────────────
        // 【第 4 章 §4.3】getStateHistory() —— 查看历史快照
        //
        // 每次节点执行后都会产生一个 Checkpoint 快照。
        // 这是"时间旅行"功能的基础。
        // ─────────────────────────────────────────────────────
        System.out.println("\n" + "-".repeat(60));
        System.out.println("  查看 user-001 的历史快照（getStateHistory）");
        System.out.println("-".repeat(60));

        var history = graph.getStateHistory(configUser1);
        int snapshotCount = 0;
        for (var snapshot : history) {
            snapshotCount++;
            System.out.printf("  快照 #%d | 消息数：%d | 下一节点：%s%n",
                    snapshotCount,
                    snapshot.state().messages().size(),
                    snapshot.config().nextNode().orElse("（结束）"));
            if (snapshotCount >= 5) {
                System.out.println("  ... （仅展示前 5 条）");
                break;
            }
        }

        // ═════════════════════════════════════════════════════
        // 场景二：新会话（不同 threadId，记忆完全隔离）
        // ═════════════════════════════════════════════════════
        System.out.println("\n" + "=".repeat(60));
        System.out.println("  场景二：新会话（threadId = user-002）");
        System.out.println("=".repeat(60));

        var configUser2 = RunnableConfig.builder()
                .threadId("user-002")    // 全新会话，与 user-001 完全隔离
                .build();

        System.out.println("\n>>> 第 1 轮：测试隔离性（user-002 不知道小明的存在）");
        runTurn(graph, configUser2, "你知道我叫什么名字吗？");

        // ═════════════════════════════════════════════════════
        // 场景三：updateState() —— 手动注入记忆
        //
        // 可以在任意时刻手动修改 State，
        // 就像某个节点执行了这次更新一样。
        // ═════════════════════════════════════════════════════
        System.out.println("\n" + "=".repeat(60));
        System.out.println("  场景三：updateState() 手动注入系统提示");
        System.out.println("=".repeat(60));

        var configUser3 = RunnableConfig.builder()
                .threadId("user-003")
                .build();

        // ✅ 修复：先用一条真实消息跑一轮，让框架创建第一个 Checkpoint
        // 这一轮就当做系统初始化，把角色设定注入进去
        System.out.println("\n>>> 初始化会话（注入系统上下文）");
        runTurn(graph, configUser3,
                "【系统上下文】你是一个专业的天气助手，用户名是「小红」，请记住这一点。");

        // 在对话开始前，手动注入一条系统级别的上下文
        // asNode 参数指定"这条更新来自哪个节点"，影响下次执行的起点
        graph.updateState(
                configUser3,
                Map.of("messages", UserMessage.from(
                        "【系统上下文】你是一个专业的天气助手，用户名是「小红」，请记住这一点。"
                )),
                "callAgent"  // 假装这是 callAgent 节点发出的更新
        );

        System.out.println("\n>>> 对话：验证手动注入的上下文");
        runTurn(graph, configUser3, "你好，请问你还记得我的名字吗？");

        System.out.println("\n" + "=".repeat(60));
        System.out.println("  Demo 运行完毕！");
        System.out.println("=".repeat(60));
    }

    /**
     * 辅助方法：向图发送一轮用户消息，并打印 Agent 最终回复。
     *
     * 使用 stream() 流式执行：
     *   - 每个节点执行完毕后立即产生一个 NodeOutput
     *   - 可实时观察状态流转，无需等待全部执行完毕
     *
     * @param graph         编译后的图实例
     * @param config        运行时配置（含 threadId）
     * @param userMessage   用户输入
     */
    private static void runTurn(
            org.bsc.langgraph4j.CompiledGraph<ChatState> graph,
            RunnableConfig config,
            String userMessage) throws Exception {

        System.out.println("用户：" + userMessage);

        // 将用户消息封装为 UserMessage 加入 State
        // AppenderChannel 会把它追加到历史消息列表末尾
        Map<String, Object> inputs = Map.of(
                "messages", UserMessage.from(userMessage)
        );

        // 【第 3 章 §2.4】stream() 流式执行
        // 返回 AsyncGenerator，每次节点执行后 yield 一个 NodeOutput
        var stream = graph.stream(inputs, config);

        String finalReply = null;
        for (var nodeOutput : stream) {
            // nodeOutput.node()  → 当前执行完毕的节点名称
            // nodeOutput.state() → 该节点执行后的完整 State 快照
            if ("callAgent".equals(nodeOutput.node())) {
                var lastMsg = nodeOutput.state().lastMessage().orElse(null);
                if (lastMsg instanceof AiMessage aiMsg && !aiMsg.hasToolExecutionRequests()) {
                    // 只打印 LLM 的最终文字回复（排除工具调用请求）
                    finalReply = aiMsg.text();
                }
            }
        }

        if (finalReply != null) {
            System.out.println("Agent：" + finalReply);
        }
    }
}