package com.mordan.aihub.fitness.domain.ai;

import lombok.Data;

import java.util.List;

/**
 * AI 返回的每日训练结果
 *
 * @author fitness
 */
@Data
public class DayResult {

    /** 星期几：1=周一 ... 7=周日 */
    private int dayOfWeek;
    /** 是否休息日 */
    private boolean isRestDay;
    /** 重点锻炼肌群 */
    private String focusMuscleGroup;
    /** 热身说明 */
    private String warmUpNotes;
    /** 拉伸放松说明 */
    private String coolDownNotes;
    /** 动作列表 */
    private List<ExerciseResult> exercises;
}
