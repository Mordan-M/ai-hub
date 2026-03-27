package com.mordan.aihub.lowcode.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mordan.aihub.lowcode.domain.entity.ConversationMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对话消息 Mapper
 */
@Mapper
public interface ConversationMessageMapper extends BaseMapper<ConversationMessage> {
}
