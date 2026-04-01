package com.mordan.aihub.lowcode.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.mordan.aihub.lowcode.domain.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 代码生成任务实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("generation_task")
public class GenerationTask {

    /** 任务ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属应用ID */
    private Long appId;

    /** 所属用户ID */
    private Long userId;

    /** 用户输入提示词 */
    private String prompt;

    /** 用户提供的API文档文本（可为空） */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private String apiDocText;

    /** 任务状态 */
    private TaskStatus status;

    /** 已重试次数 */
    private Integer retryCount;

    /** 最大重试次数 */
    private Integer maxRetry;

    /** LangGraph4j threadId */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private String workflowThreadId;

    /** 失败原因 */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private String errorMessage;

    /** 创建时间戳（毫秒） */
    @TableField(fill = FieldFill.INSERT)
    private Long createdAt;

    /** 完成时间戳（毫秒） */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Long finishedAt;
}
