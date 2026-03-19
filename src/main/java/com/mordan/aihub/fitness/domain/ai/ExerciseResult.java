package com.mordan.aihub.fitness.domain.ai;

import lombok.Data;

/**
 * AI 返回的健身动作结果
 *
 * @author fitness
 */
@Data
public class ExerciseResult {

    /** 动作中文名 */
    private String nameZh;
    /** 动作英文名 */
    private String nameEn;
    /** 组数 */
    private int sets;
    /** 次数或时长，如：10-12 或 30s */
    private String reps;
    /** 组间休息秒数 */
    private int restSeconds;
    /** AI生成的动作要点提示 */
    private String coachNotes;
}
