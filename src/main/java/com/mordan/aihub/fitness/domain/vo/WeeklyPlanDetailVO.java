package com.mordan.aihub.fitness.domain.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 周计划详情 VO
 *
 * @author fitness
 */
@Data
public class WeeklyPlanDetailVO {

    /** 计划ID */
    private Long planId;
    /** 计划标题 */
    private String title;
    /** 整体计划说明 */
    private String summary;
    /** 计划开始日期（本周周一） */
    private LocalDate weekStartDate;
    /** 生成状态：generating=生成中/done=完成/failed=失败 */
    private String status;
    /** 每日训练列表 */
    private List<DailyTrainingVO> days;
}
