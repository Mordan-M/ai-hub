package com.mordan.aihub.lowcode.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * @className: RepairCodeAiService
 * @description: 代码修复 ai 服务接口
 * @author: 91002183
 * @date: 2026/3/31
 **/
public interface RepairCodeAiService {

    /**
     * 代码修复：根据错误列表修复代码
     * @param appId 应用ID（用于记忆）
     * @param userPrompt 错误列表+优化建议+原始代码
     * @return JSON 结果 {"files":[{"path":"...","content":"..."}]}
     */
    @SystemMessage(fromResource = "prompts/lowcode/repair-code-system-prompt.txt")
    String repairCode(@MemoryId String appId, @UserMessage String userPrompt);

}
