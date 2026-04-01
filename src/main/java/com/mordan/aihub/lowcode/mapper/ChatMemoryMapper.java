package com.mordan.aihub.lowcode.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mordan.aihub.lowcode.domain.entity.ChatMemory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对话记忆 Mapper
 */
@Mapper
public interface ChatMemoryMapper extends BaseMapper<ChatMemory> {
}
