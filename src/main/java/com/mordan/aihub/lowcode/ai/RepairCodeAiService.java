package com.mordan.aihub.lowcode.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 代码修复 AI 服务
 */
public interface RepairCodeAiService {

    @SystemMessage(fromResource = "prompts/lowcode/repair-code-system-prompt.txt")
    String repairCode(@UserMessage String userPrompt);
}
