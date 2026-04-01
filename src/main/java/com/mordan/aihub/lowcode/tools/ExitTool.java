package com.mordan.aihub.lowcode.tools;

import cn.hutool.json.JSONObject;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 告诉 AI 要退出的工具
 */
@Slf4j
@Component
public class ExitTool extends BaseTool {

    @Override
    public String getToolName() {
        return "exit";
    }

    @Override
    public String getDisplayName() {
        return "退出工具调用";
    }

    /**
     * 退出工具调用
     * 当所有修改完成后调用此方法，然后输出最终的 JSON 结果
     *
     * @return 退出确认信息
     */
    @Tool("所有修改完成后，使用此工具退出工具调用。调用后请输出最终的 JSON 结果，格式必须符合要求：{\"summary\": {...}, \"files\": [...]}")
    public String exit() {
        log.info("AI 请求退出工具调用，准备输出最终 JSON");
        return "修改完成，停止工具调用，请输出最终的 JSON 结果，格式必须包含 summary 和 files";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        return "\n\n[执行结束]\n\n";
    }
}