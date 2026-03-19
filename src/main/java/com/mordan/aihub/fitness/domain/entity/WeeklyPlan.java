package com.mordan.aihub.fitness.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * AI 生成的周计划实体类
 *
 * @author fitness
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("weekly_plans")
public class WeeklyPlan {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联用户ID */
    private Long userId;

    /** 关联用户偏好ID */
    private Long preferenceId;

    /** 计划标题 */
    private String title;

    /** AI生成的整体计划说明 */
    private String summary;

    /** 计划开始日期（本周周一） */
    private LocalDate weekStartDate;

    /** 生成状态：generating=生成中/done=完成/failed=失败 */
    private String status;

    /** 逻辑删除：0=未删除 1=已删除 */
    @TableLogic
    private Byte deleted;

    /** 创建时间戳（毫秒） */
    @TableField(fill = FieldFill.INSERT)
    private Long createdAt;

    /** 更新时间戳（毫秒） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedAt;

    /** 状态：生成中 */
    public static final String STATUS_GENERATING = "generating";
    /** 状态：生成完成 */
    public static final String STATUS_DONE = "done";
    /** 状态：生成失败 */
    public static final String STATUS_FAILED = "failed";
}