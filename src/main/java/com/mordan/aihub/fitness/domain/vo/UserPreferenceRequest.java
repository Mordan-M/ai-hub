package com.mordan.aihub.fitness.domain.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 用户健身偏好请求入参
 *
 * @author fitness
 */
@Data
public class UserPreferenceRequest {

    /** 经验等级：beginner=新手/intermediate=进阶/advanced=老手 */
    @NotBlank(message = "经验等级不能为空")
    private String experienceLevel;

    /** 训练目标：muscle_gain=增肌/fat_loss=减脂/body_shaping=塑形/endurance=提升耐力/general_health=保持健康 */
    @NotBlank(message = "训练目标不能为空")
    private String goal;

    /** 重点锻炼部位列表 */
    @NotNull(message = "重点锻炼部位不能为空")
    private List<String> focusMuscles;

    /** 可用器材：none=无器械/dumbbell=哑铃/barbell=杠铃/gym_machine=健身房全套/home_equipment=家用器材 */
    @NotBlank(message = "可用器材不能为空")
    private String equipment;

    /** 单次训练时长：under_30=30分钟以内/30_to_60=30-60分钟/60_to_90=60-90分钟/above_90=90分钟以上 */
    @NotBlank(message = "训练时长不能为空")
    private String sessionDuration;

    /** 每周训练天数 */
    @NotNull(message = "每周训练天数不能为空")
    private Integer trainingDaysPerWeek;

    /** 训练风格：split=分化训练/full_body=全身训练/hiit=高强度间歇/circuit=循环训练/strength=力量训练 */
    @NotBlank(message = "训练风格不能为空")
    private String trainingStyle;

    /** 受伤或需要规避的部位说明 */
    private String injuryNotes;
}
