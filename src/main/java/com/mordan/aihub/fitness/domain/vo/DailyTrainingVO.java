package com.mordan.aihub.fitness.domain.vo;

import java.util.List;

/**
 * 每日训练详情 VO
 *
 * @author fitness
 */
public record DailyTrainingVO(
    /** 星期几：1=周一 ... 7=周日 */
    Integer dayOfWeek,
    /** 是否休息日 */
    Boolean isRestDay,
    /** 重点锻炼肌群 */
    String focusMuscleGroup,
    /** 热身说明 */
    String warmUpNotes,
    /** 拉伸放松说明 */
    String coolDownNotes,
    /** 动作列表 */
    List<ExerciseItemVO> exercises
) {}
