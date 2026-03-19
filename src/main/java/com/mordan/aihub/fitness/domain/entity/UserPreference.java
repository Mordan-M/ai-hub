package com.mordan.aihub.fitness.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户健身偏好实体类
 *
 * @author fitness
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_preferences")
public class UserPreference {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联用户ID */
    private Long userId;

    /** 经验等级：beginner=新手/intermediate=进阶/advanced=老手 */
    private String experienceLevel;

    /** 训练目标：muscle_gain=增肌/fat_loss=减脂/body_shaping=塑形/endurance=提升耐力/general_health=保持健康 */
    private String goal;

    /** 重点锻炼部位（JSON数组） */
    private String focusMuscles;

    /** 可用器材：none=无器械/dumbbell=哑铃/barbell=杠铃/gym_machine=健身房全套/home_equipment=家用器材 */
    private String equipment;

    /** 单次训练时长：under_30=30分钟以内/30_to_60=30-60分钟/60_to_90=60-90分钟/above_90=90分钟以上 */
    private String sessionDuration;

    /** 每周训练天数 */
    private Integer trainingDaysPerWeek;

    /** 训练风格：split=分化训练/full_body=全身训练/hiit=高强度间歇/circuit=循环训练/strength=力量训练 */
    private String trainingStyle;

    /** 受伤或需要规避的部位说明 */
    private String injuryNotes;

    /** 逻辑删除：0=未删除 1=已删除 */
    @TableLogic
    private Byte deleted;

    /** 创建时间戳（毫秒） */
    @TableField(fill = FieldFill.INSERT)
    private Long createdAt;

    /** 更新时间戳（毫秒） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedAt;
}