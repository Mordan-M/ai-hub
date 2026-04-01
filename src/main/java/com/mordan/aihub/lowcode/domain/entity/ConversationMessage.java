package com.mordan.aihub.lowcode.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.mordan.aihub.lowcode.domain.enums.MessageRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对话消息实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("conversation_message")
public class ConversationMessage {

    /** 消息ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属应用ID */
    private Long appId;

    /** 所属用户ID */
    private Long userId;

    /** 消息顺序号 */
    private Integer seq;

    /** 消息角色 */
    private MessageRole role;

    /** 消息内容（纯文本，用户输入或AI回复） */
    private String content;

    /** 关联的生成任务ID */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Long taskId;

    /** 创建时间戳（毫秒） */
    @TableField(fill = FieldFill.INSERT)
    private Long createdAt;
}
