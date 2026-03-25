package com.mordan.aihub.studydemo.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.Map;

/**
 * @className: WeatherTool
 * @description: 天气工具
 * @author: 91002183
 * @date: 2026/3/25
 **/
public class WeatherTool {

    // ─────────────────────────────────────────────────────────
    // 【第 3 章 §3.3】定义工具（Tool）
    //
    // 工具就是普通 Java 方法，加 @Tool 注解即可被 LangChain4j 识别。
    // LangGraph4j 的 invokeTool 节点会在 LLM 请求工具调用时执行它。
    // ─────────────────────────────────────────────────────────
    @Tool("查询指定城市的实时天气情况")
    String getWeather(@P("城市名称，例如：北京、上海") String city) {
        // 模拟天气 API（真实场景中调用外部 API）
        Map<String, String> fakeWeather = Map.of(
                "北京", "晴，25°C，东南风 3 级",
                "上海", "多云，22°C，东风 2 级",
                "广州", "小雨，28°C，西南风 2 级"
        );
        return fakeWeather.getOrDefault(city, "暂无 " + city + " 的天气数据");
    }

}
