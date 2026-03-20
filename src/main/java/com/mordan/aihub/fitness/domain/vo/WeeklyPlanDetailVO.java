package com.mordan.aihub.fitness.domain.vo;

import java.time.LocalDate;
import java.util.List;

/**
 * 周计划详情 VO
 *
 * @author fitness
 */
public record WeeklyPlanDetailVO(
    /** 计划ID */
    Long planId,
    /** 计划标题 */
    String title,
    /** 整体计划说明 */
    String summary,
    /** 计划开始日期（本周周一） */
    LocalDate weekStartDate,
    /** 生成状态：generating=生成中/done=完成/failed=失败 */
    String status,
    /** 每日训练列表 */
    List<DailyTrainingVO> days
) {}
