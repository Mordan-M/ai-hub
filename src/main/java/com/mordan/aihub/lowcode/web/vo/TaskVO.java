package com.mordan.aihub.lowcode.web.vo;

import com.mordan.aihub.lowcode.domain.enums.TaskStatus;
import lombok.Builder;
import lombok.Data;

/**
 * 生成任务VO
 */
@Data
@Builder
public class TaskVO {

    /** 任务ID */
    private Long id;

    /** 应用ID */
    private Long appId;

    /** 任务状态 */
    private TaskStatus status;

    /** 已重试次数 */
    private Integer retryCount;

    /** 错误信息 */
    private String errorMessage;

    /** 创建时间戳 */
    private Long createdAt;

    /** 完成时间戳 */
    private Long finishedAt;
}
