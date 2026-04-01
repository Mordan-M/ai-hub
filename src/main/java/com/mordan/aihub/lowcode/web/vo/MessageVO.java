package com.mordan.aihub.lowcode.web.vo;

import com.mordan.aihub.lowcode.domain.enums.MessageRole;
import lombok.Builder;
import lombok.Data;

/**
 * 对话消息VO
 */
@Data
@Builder
public class MessageVO {

    /** 消息ID */
    private Long id;

    /** 消息角色 */
    private MessageRole role;

    /** 消息内容 */
    private String content;

    /** 关联任务ID */
    private Long taskId;

    /** 预览地址（成功生成时才有） */
    private String previewUrl;

    /** 创建时间戳 */
    private Long createdAt;
}
