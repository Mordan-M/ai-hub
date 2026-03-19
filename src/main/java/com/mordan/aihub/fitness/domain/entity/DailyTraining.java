package com.mordan.aihub.fitness.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 每日训练实体类
 *
 * @author fitness
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("daily_trainings")
public class DailyTraining {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联周计划ID */
    private Long planId;

    /** 星期几：1=周一 ... 7=周日 */
    private Integer dayOfWeek;

    /** 是否休息日：0=训练日 1=休息日 */
    private Byte isRestDay;

    /** 重点锻炼肌群 */
    private String focusMuscleGroup;

    /** 热身说明 */
    private String warmUpNotes;

    /** 拉伸放松说明 */
    private String coolDownNotes;

    /** 创建时间戳（毫秒） */
    @TableField(fill = FieldFill.INSERT)
    private Long createdAt;

    /** 更新时间戳（毫秒） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedAt;
}