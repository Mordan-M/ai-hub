package com.mordan.aihub.lowcode.domain.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mordan.aihub.lowcode.domain.entity.ChatMemory;

/**
 * 对话记忆服务接口
 * 专门处理 LangChain4j 对话记忆的持久化存储
 */
public interface ChatMemoryService extends IService<ChatMemory> {
}
