package com.mordan.aihub.fitness.domain.ai;

/**
 * AI 返回的健身动作结果
 *
 * @author fitness
 */
public record ExerciseResult(
    /** 动作中文名 */
    String nameZh,
    /** 动作英文名 */
    String nameEn,
    /** 组数 */
    int sets,
    /** 次数或时长，如：10-12 或 30s */
    String reps,
    /** 组间休息秒数 */
    int restSeconds,
    /** AI生成的动作要点提示 */
    String coachNotes
) {}
