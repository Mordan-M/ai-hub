package com.mordan.aihub.studydemo.langgraph;

/**
 * ============================================================
 *  LangGraph4j 进阶阶段 Demo：复杂 Agent 可视化调试
 * ============================================================
 *
 * 本 Demo 完整覆盖第 5-7 章核心知识点：
 *
 *  ┌─────────────────────────────────────────────────────────┐
 *  │  第 5 章：高级特性                                        │
 *  │  ✅ Hooks —— BeforeCall / AfterCall / WrapCall           │
 *  │  ✅ 全局 Hook & 指定节点 Hook                             │
 *  │  ✅ Edge Hook（条件边拦截）                               │
 *  │  ✅ 并行分支（Parallel Branch）+ ForkJoinPool             │
 *  │  ✅ 子图（Subgraph）—— 编译子图作为父图节点               │
 *  │                                                         │
 *  │  第 6 章：图可视化                                        │
 *  │  ✅ 导出 Mermaid 格式                                     │
 *  │  ✅ 导出 PlantUML 格式                                    │
 *  │                                                         │
 *  │  第 7 章：Studio（嵌入式可视化调试）                      │
 *  │  ✅ GraphRepresentation 获取图结构                        │
 *  │  ✅ stream() 流式执行 + NodeOutput 逐步观察               │
 *  └─────────────────────────────────────────────────────────┘
 *
 * 场景说明（新闻分析 Agent）：
 *
 *   用户输入一个话题 → 路由节点决定 → 并行执行「情感分析」和「关键词提取」
 *   两路结果汇聚 → 子图「摘要生成器」生成最终摘要 → 输出
 *
 *   整个流程挂载 Hooks 实现：
 *     - 全局耗时统计（WrapCall Hook）
 *     - 节点入参/出参打印（BeforeCall / AfterCall Hook）
 *     - 条件边路由决策记录（Edge AfterCall Hook）
 *
 * 图结构：
 *
 *   START
 *     │
 *     ▼
 *  [router]  ──── 条件边 ──► [fallback] ──► END
 *     │ （话题有效）
 *     ▼
 *  [dispatcher]──────────────────────────┐
 *     │                                  │
 *     ▼                                  ▼
 *  [sentimentAnalyzer]          [keywordExtractor]   ← 并行执行
 *     │                                  │
 *     └──────────────┬───────────────────┘
 *                    ▼
 *              [summaryGraph]  ← 编译子图
 *              （内部：summarize → polish）
 *                    │
 *                    ▼
 *                   END
 */

import com.mordan.aihub.studydemo.langgraph.state.AdvancedState;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.EdgeAction;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.bsc.langgraph4j.hook.EdgeHook;
import org.bsc.langgraph4j.hook.NodeHook;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicLong;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Component
@Slf4j
public class AdvancedAgentWorkFlow {

    @Resource
    private ChatModel chatModel;

    public void executeWorkflow() throws Exception {

        // ═════════════════════════════════════════════════════
        // 【第 5 章 §5.3】构建「摘要生成」子图（Subgraph）
        //
        // 子图是独立的 StateGraph，编译后作为父图的一个节点使用。
        // 优点：逻辑封装、可复用、可独立测试。
        //
        // 子图内部流程：
        //   START → [summarize] → [polish] → END
        // ═════════════════════════════════════════════════════

        // ── 子图节点 1：summarize —— 生成初稿摘要 ──
        var summarize = node_async((AdvancedState state) -> {
            log.info("[子图/summarize] 开始生成摘要，分析结果数量：{}", state.analysisResults().size());

            // 将并行分析结果拼接为 prompt
            var analysisText = String.join("\n", state.analysisResults());
            var prompt = "根据以下分析结果，生成一段简洁的新闻摘要（100字以内）：\n" + analysisText;

            var response = chatModel.chat(
                    ChatRequest.builder()
                            .messages(UserMessage.from(prompt))
                            .build()
            );
            var summary = response.aiMessage().text();
            log.info("[子图/summarize] 初稿摘要：{}", summary);

            return Map.of("summary", summary);
        });

        // ── 子图节点 2：polish —— 润色摘要 ──
        var polish = node_async((AdvancedState state) -> {
            log.info("[子图/polish] 开始润色摘要");

            var prompt = "请对以下摘要进行语言润色，使其更流畅自然，保持原意：\n" + state.summary();

            var response = chatModel.chat(
                    ChatRequest.builder()
                            .messages(UserMessage.from(prompt))
                            .build()
            );
            var polished = response.aiMessage().text();
            log.info("[子图/polish] 润色完成：{}", polished);

            return Map.of("summary", polished);
        });

        // ── 构建并编译子图 ──
        var summarySubGraph = new StateGraph<>(AdvancedState.SCHEMA, AdvancedState::new)
                .addNode("summarize", summarize)
                .addNode("polish", polish)
                .addEdge(START, "summarize")
                .addEdge("summarize", "polish")
                .addEdge("polish", END)
                .compile();

        log.info("\n【子图结构 - Mermaid】\n{}",
                summarySubGraph.getGraph(GraphRepresentation.Type.MERMAID, "摘要子图").content());

        // ═════════════════════════════════════════════════════
        // 【第 5 章 §5.1】定义父图节点
        // ═════════════════════════════════════════════════════

        // ── 节点：router —— 校验话题有效性 ──
        var router = node_async((AdvancedState state) -> {
            log.info("[router] 收到话题：{}", state.topic());
            // 简单校验：话题不为空且长度 > 2
            boolean valid = state.topic() != null && state.topic().trim().length() > 2;
            return Map.of("valid", valid);
        });

        // ── 节点：fallback —— 无效话题兜底 ──
        var fallback = node_async((AdvancedState state) -> {
            log.warn("[fallback] 话题无效，返回默认提示");
            return Map.of("summary", "话题无效，请输入有效的新闻话题（长度 > 2）。");
        });

        // ── 节点：dispatcher —— 分发任务（并行分支起点） ──
        var dispatcher = node_async((AdvancedState state) -> {
            log.info("[dispatcher] 分发任务，话题：{}", state.topic());
            // dispatcher 本身不做处理，仅作为并行分支的 fork 节点
            return Map.of();
        });

        // ── 节点：sentimentAnalyzer —— 情感分析 ──
        var sentimentAnalyzer = node_async((AdvancedState state) -> {
            log.info("[sentimentAnalyzer] 开始情感分析，话题：{}", state.topic());

            var prompt = "对话题「" + state.topic() + "」进行情感倾向分析，" +
                    "判断是正面/负面/中性，并给出简短理由（30字以内）。直接输出结论。";

            var response = chatModel.chat(
                    ChatRequest.builder()
                            .messages(UserMessage.from(prompt))
                            .build()
            );
            var result = "【情感分析】" + response.aiMessage().text();
            log.info("[sentimentAnalyzer] 结果：{}", result);

            // 通过 AppenderChannel 追加到 analysisResults 列表
            return Map.of("analysisResults", result);
        });

        // ── 节点：keywordExtractor —— 关键词提取 ──
        var keywordExtractor = node_async((AdvancedState state) -> {
            log.info("[keywordExtractor] 开始关键词提取，话题：{}", state.topic());

            var prompt = "从话题「" + state.topic() + "」中提取 3-5 个核心关键词，用逗号分隔，直接输出关键词。";

            var response = chatModel.chat(
                    ChatRequest.builder()
                            .messages(UserMessage.from(prompt))
                            .build()
            );
            var result = "【关键词】" + response.aiMessage().text();
            log.info("[keywordExtractor] 结果：{}", result);

            return Map.of("analysisResults", result);
        });

        // ─────────────────────────────────────────────────────
        // 【第 3 章 §3.4】定义条件边
        // ─────────────────────────────────────────────────────
        EdgeAction<AdvancedState> routeByValidity = state ->
                state.valid() ? "valid" : "invalid";

        // ═════════════════════════════════════════════════════
        // 【第 5 章 §5.1】注册 Hooks
        //
        // Hook 注册在 StateGraph 上，编译时生效。
        // 三种类型：
        //   BeforeCall —— 节点执行前触发（LIFO 顺序）
        //   AfterCall  —— 节点执行后触发（LIFO 顺序）
        //   WrapCall   —— 包裹节点，可控制是否调用原逻辑（FIFO 顺序）
        // ═════════════════════════════════════════════════════

        // ── Hook 1：全局 WrapCall —— 统计每个节点的执行耗时 ──
        //
        // WrapCall 最强大：它接收原始 action，自己决定何时调用它。
        // 执行顺序 FIFO：先注册的先"包裹"，其 before 逻辑先执行，after 逻辑最后执行。
        NodeHook.WrapCall<AdvancedState> timingHook = (nodeId, state, config, action) -> {
            long startMs = System.currentTimeMillis();
            log.info("⏱ [HOOK-WrapCall] 节点 [{}] 开始执行", nodeId);

            // 调用原始节点逻辑
            return action.apply(state, config).thenApply(result -> {
                long costMs = System.currentTimeMillis() - startMs;
                log.info("⏱ [HOOK-WrapCall] 节点 [{}] 执行完毕，耗时 {} ms，输出 key：{}",
                        nodeId, costMs, result.keySet());
                return result;
            });
        };

        // ── Hook 2：全局 BeforeCall —— 打印节点入参（状态摘要）──
        //
        // BeforeCall 接收当前 state，返回 Map（可用来在节点执行前修改 state）。
        // 返回空 Map 表示不修改。
        NodeHook.BeforeCall<AdvancedState> beforeHook = (nodeId, state, config) -> {
            log.debug("📥 [HOOK-BeforeCall] 节点 [{}] 当前 topic={}，analysisResults 数量={}",
                    nodeId, state.topic(), state.analysisResults().size());
            // 返回空 Map 表示不对 state 做任何修改
            return CompletableFuture.completedFuture(Map.of());
        };

        // ── Hook 3：指定节点 AfterCall —— 只在 sentimentAnalyzer 后记录 ──
        //
        // AfterCall 除了 state，还接收节点的返回值 lastResult。
        // 可用来做结果审计、告警等。
        NodeHook.AfterCall<AdvancedState> sentimentAfterHook = (nodeId, state, config, lastResult) -> {
            log.info("📤 [HOOK-AfterCall] sentimentAnalyzer 输出：{}", lastResult);
            // 同样返回 Map，可以在这里修改结果；返回原 lastResult 表示不修改
            return CompletableFuture.completedFuture(lastResult);
        };

        // ── Hook 4：Edge AfterCall —— 记录条件边的路由决策 ──
        EdgeHook.AfterCall<AdvancedState> edgeAfterHook = (sourceId, state, config, command) -> {
            log.info("🔀 [HOOK-EdgeAfterCall] 条件边 [{}] 路由决策：{}", sourceId, command);
            return CompletableFuture.completedFuture(command);
        };

        // ═════════════════════════════════════════════════════
        // 构建父图，并注册所有 Hook
        // ═════════════════════════════════════════════════════
        var workflow = new StateGraph<>(AdvancedState.SCHEMA, AdvancedState::new)

                // ── 添加节点 ──
                .addNode("router", router)
                .addNode("fallback", fallback)
                .addNode("dispatcher", dispatcher)
                .addNode("sentimentAnalyzer", sentimentAnalyzer)
                .addNode("keywordExtractor", keywordExtractor)
                // 【第 5 章 §5.3】将编译好的子图作为父图的一个节点
                .addNode("summaryGraph", summarySubGraph)

                // ── 添加边 ──
                .addEdge(START, "router")
                // 条件边：router → dispatcher（有效）or fallback（无效）
                .addConditionalEdges(
                        "router",
                        edge_async(routeByValidity),
                        Map.of("valid", "dispatcher", "invalid", "fallback")
                )
                .addEdge("fallback", END)
                // 【第 5 章 §5.2】并行分支：dispatcher 同时触发两个分析节点
                // 同一个源节点 addEdge 两次 → 框架自动识别为并行分支
                .addEdge("dispatcher", "sentimentAnalyzer")
                .addEdge("dispatcher", "keywordExtractor")
                // 两个并行节点都汇聚到 summaryGraph
                .addEdge("sentimentAnalyzer", "summaryGraph")
                .addEdge("keywordExtractor", "summaryGraph")
                .addEdge("summaryGraph", END)

                // ── 注册 Hooks ──
                // 全局 WrapCall（作用于所有节点）
                .addWrapCallNodeHook(timingHook)
                // 全局 BeforeCall（作用于所有节点）
                .addBeforeCallNodeHook(beforeHook)
                // 指定节点 AfterCall（只作用于 sentimentAnalyzer）
                .addAfterCallNodeHook("sentimentAnalyzer", sentimentAfterHook)
                // 条件边的 AfterCall Hook（作用于所有条件边）
                .addAfterCallEdgeHook(edgeAfterHook);

        // ─────────────────────────────────────────────────────
        // 【第 6 章】图可视化 —— 编译前导出（StateGraph 级别）
        // ─────────────────────────────────────────────────────
        log.info("\n【父图结构 - Mermaid】\n{}",
                workflow.getGraph(GraphRepresentation.Type.MERMAID, "新闻分析 Agent").content());

        log.info("\n【父图结构 - PlantUML】\n{}",
                workflow.getGraph(GraphRepresentation.Type.PLANTUML, "新闻分析 Agent").content());

        // ─────────────────────────────────────────────────────
        // 编译图
        // ─────────────────────────────────────────────────────
        var memorySaver = new MemorySaver();
        var compileConfig = CompileConfig.builder()
                .checkpointSaver(memorySaver)
                .recursionLimit(20)
                .build();

        var graph = workflow.compile(compileConfig);

        // 编译后也可以导出（CompiledGraph 级别，子图已合并）
        log.info("\n【编译后图结构 - Mermaid】\n{}",
                graph.getGraph(GraphRepresentation.Type.MERMAID, "新闻分析 Agent（编译后）").content());

        // ═════════════════════════════════════════════════════
        // 场景一：正常话题 —— 触发并行分析 + 子图摘要
        // ═════════════════════════════════════════════════════
        log.info("\n{}\n  场景一：正常话题，触发完整工作流\n{}", "=".repeat(60), "=".repeat(60));

        var config1 = RunnableConfig.builder()
                .threadId("session-001")
                // 【第 5 章 §5.2】为 dispatcher 节点配置并行执行器
                // dispatcher 完成后，其后继节点（sentimentAnalyzer、keywordExtractor）将并行运行
                .addParallelNodeExecutor("dispatcher", ForkJoinPool.commonPool())
                .build();

        // 【第 7 章】stream() 流式执行，逐步观察每个节点的执行结果
        runAndPrint(graph, config1, Map.of("topic", "人工智能在医疗领域的最新突破"));

        // ═════════════════════════════════════════════════════
        // 场景二：无效话题 —— 触发 fallback 兜底路径
        // ═════════════════════════════════════════════════════
        log.info("\n{}\n  场景二：无效话题，触发 fallback\n{}", "=".repeat(60), "=".repeat(60));

        var config2 = RunnableConfig.builder()
                .threadId("session-002")
                .build();

        runAndPrint(graph, config2, Map.of("topic", "AI"));  // 长度 <= 2，触发 fallback

        // ═════════════════════════════════════════════════════
        // 场景三：演示 Hook 执行顺序（WrapCall FIFO）
        //
        // 注册两个 WrapCall Hook，验证 FIFO 顺序：
        //   Hook-W1 先注册 → 其 before 先执行，after 最后执行
        //   Hook-W2 后注册 → 其 before 后执行，after 先执行
        // 执行顺序：W1-before → W2-before → [节点] → W2-after → W1-after
        // ═════════════════════════════════════════════════════
        log.info("\n{}\n  场景三：验证 WrapCall Hook FIFO 执行顺序\n{}", "=".repeat(60), "=".repeat(60));

        AtomicLong orderCounter = new AtomicLong(0);

        NodeHook.WrapCall<AdvancedState> wrapHook1 = (nodeId, state, config, action) -> {
            if ("router".equals(nodeId)) {  // 只在 router 节点演示，避免日志过多
                long order = orderCounter.incrementAndGet();
                log.info("🔵 [Hook-W1 before] 第 {} 步执行，节点：{}", order, nodeId);
                return action.apply(state, config).thenApply(result -> {
                    long afterOrder = orderCounter.incrementAndGet();
                    log.info("🔵 [Hook-W1 after]  第 {} 步执行，节点：{}", afterOrder, nodeId);
                    return result;
                });
            }
            return action.apply(state, config);
        };

        NodeHook.WrapCall<AdvancedState> wrapHook2 = (nodeId, state, config, action) -> {
            if ("router".equals(nodeId)) {
                long order = orderCounter.incrementAndGet();
                log.info("🟠 [Hook-W2 before] 第 {} 步执行，节点：{}", order, nodeId);
                return action.apply(state, config).thenApply(result -> {
                    long afterOrder = orderCounter.incrementAndGet();
                    log.info("🟠 [Hook-W2 after]  第 {} 步执行，节点：{}", afterOrder, nodeId);
                    return result;
                });
            }
            return action.apply(state, config);
        };

        // 注册两个 WrapCall，验证 FIFO
        var workflowForHookDemo = new StateGraph<>(AdvancedState.SCHEMA, AdvancedState::new)
                .addNode("router", router)
                .addNode("fallback", fallback)
                .addNode("dispatcher", dispatcher)
                .addNode("sentimentAnalyzer", sentimentAnalyzer)
                .addNode("keywordExtractor", keywordExtractor)
                .addNode("summaryGraph", summarySubGraph)
                .addEdge(START, "router")
                .addConditionalEdges("router", edge_async(routeByValidity),
                        Map.of("valid", "dispatcher", "invalid", "fallback"))
                .addEdge("fallback", END)
                .addEdge("dispatcher", "sentimentAnalyzer")
                .addEdge("dispatcher", "keywordExtractor")
                .addEdge("sentimentAnalyzer", "summaryGraph")
                .addEdge("keywordExtractor", "summaryGraph")
                .addEdge("summaryGraph", END)
                // Hook-W1 先注册（FIFO：before 先执行，after 最后执行）
                .addWrapCallNodeHook(wrapHook1)
                // Hook-W2 后注册（FIFO：before 后执行，after 先执行）
                .addWrapCallNodeHook(wrapHook2);

        var graphForDemo = workflowForHookDemo.compile(
                CompileConfig.builder().checkpointSaver(new MemorySaver()).build()
        );

        var config3 = RunnableConfig.builder()
                .threadId("session-003")
                .addParallelNodeExecutor("dispatcher", ForkJoinPool.commonPool())
                .build();

        log.info("预期顺序：W1-before(1) → W2-before(2) → [router] → W2-after(3) → W1-after(4)");
        runAndPrint(graphForDemo, config3, Map.of("topic", "气候变化与全球治理"));

        log.info("\n{}\n  Demo 运行完毕！\n{}", "=".repeat(60), "=".repeat(60));
    }

    /**
     * 【第 7 章】流式执行并逐步打印每个节点的输出
     *
     * stream() 返回 AsyncGenerator，每次节点执行完毕 yield 一个 NodeOutput：
     *   - nodeOutput.node()  → 节点名
     *   - nodeOutput.state() → 该节点执行后的完整 State 快照
     *
     * 这正是 Studio 工作的底层机制：Studio 消费这个流，
     * 实时高亮当前节点并展示状态数据。
     */
    private void runAndPrint(
            org.bsc.langgraph4j.CompiledGraph<AdvancedState> graph,
            RunnableConfig config,
            Map<String, Object> inputs) throws Exception {

        log.info("▶ 开始执行，输入：{}", inputs);

        var stream = graph.stream(inputs, config);

        for (var nodeOutput : stream) {
            String nodeName = nodeOutput.node();
            AdvancedState state = nodeOutput.state();

            // 跳过 START / END 虚拟节点的日志
            if ("__START__".equals(nodeName) || "__END__".equals(nodeName)) continue;

            log.info("✅ 节点 [{}] 执行完毕", nodeName);

            // 根据节点名选择性打印关键字段
            switch (nodeName) {
                case "router"      -> log.info("   valid={}", state.valid());
                case "fallback"    -> log.info("   summary（兜底）={}", state.summary());
                case "sentimentAnalyzer", "keywordExtractor"
                                   -> log.info("   analysisResults 当前数量={}", state.analysisResults().size());
                case "polish"      -> log.info("   最终摘要={}", state.summary());
                default            -> {}
            }
        }

        log.info("◼ 执行结束\n");
    }
}
