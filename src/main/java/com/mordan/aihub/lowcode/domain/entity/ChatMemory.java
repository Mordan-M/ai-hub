package com.mordan.aihub.lowcode.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mordan.aihub.lowcode.domain.enums.MessageRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LangChain4j 对话记忆实体
 * 专门存储持久化的 AI 对话记忆，与业务消息分离
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("chat_memory")
public class ChatMemory {

    /** 记忆ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 记忆ID（通常为 appId 字符串） */
    private String memoryId;

    /** 消息顺序号 */
    private Integer seq;

    /** 消息角色 */
    private MessageRole role;

    /** LangChain4j ChatMessage 序列化 JSON */
    private String content;

    /** 纯文本内容 */
    private String contentText;

    /** 创建时间戳（毫秒） */
    private Long createdAt;
}
