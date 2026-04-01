package com.mordan.aihub.lowcode.domain.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mordan.aihub.lowcode.domain.entity.ChatMemory;
import com.mordan.aihub.lowcode.domain.service.ChatMemoryService;
import com.mordan.aihub.lowcode.mapper.ChatMemoryMapper;
import org.springframework.stereotype.Service;

/**
 * 对话记忆服务实现
 */
@Service
public class ChatMemoryServiceImpl extends ServiceImpl<ChatMemoryMapper, ChatMemory>
        implements ChatMemoryService {
}
