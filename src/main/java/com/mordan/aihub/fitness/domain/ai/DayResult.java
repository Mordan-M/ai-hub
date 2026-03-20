package com.mordan.aihub.fitness.domain.ai;

import java.util.List;

/**
 * AI 返回的每日训练结果
 *
 * @author fitness
 */
public record DayResult(
    /** 星期几：1=周一 ... 7=周日 */
    int dayOfWeek,
    /** 是否休息日 */
    boolean isRestDay,
    /** 重点锻炼肌群 */
    String focusMuscleGroup,
    /** 热身说明 */
    String warmUpNotes,
    /** 拉伸放松说明 */
    String coolDownNotes,
    /** 动作列表 */
    List<ExerciseResult> exercises
) {}
