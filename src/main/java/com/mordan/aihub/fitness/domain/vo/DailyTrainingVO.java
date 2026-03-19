package com.mordan.aihub.fitness.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 每日训练详情 VO
 *
 * @author fitness
 */
@Data
public class DailyTrainingVO {

    /** 星期几：1=周一 ... 7=周日 */
    private Integer dayOfWeek;
    /** 是否休息日 */
    private Boolean isRestDay;
    /** 重点锻炼肌群 */
    private String focusMuscleGroup;
    /** 热身说明 */
    private String warmUpNotes;
    /** 拉伸放松说明 */
    private String coolDownNotes;
    /** 动作列表 */
    private List<ExerciseItemVO> exercises;
}
