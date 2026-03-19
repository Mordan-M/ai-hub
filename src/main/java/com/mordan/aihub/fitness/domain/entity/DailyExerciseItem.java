package com.mordan.aihub.fitness.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 每日具体动作实体类
 *
 * @author fitness
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("daily_exercise_items")
public class DailyExerciseItem {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联每日训练ID */
    private Long dailyTrainingId;

    /** 动作中文名 */
    private String nameZh;

    /** 动作英文名 */
    private String nameEn;

    /** 排序顺序 */
    private Integer sortOrder;

    /** 组数 */
    private Integer sets;

    /** 次数或时长，如：10-12 或 30s */
    private String reps;

    /** 组间休息秒数 */
    private Integer restSeconds;

    /** AI生成的动作要点提示 */
    private String coachNotes;

    /** B站视频搜索跳转链接 */
    private String bilibiliUrl;

    /** 创建时间戳（毫秒） */
    @TableField(fill = FieldFill.INSERT)
    private Long createdAt;

    /** 更新时间戳（毫秒） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedAt;
}