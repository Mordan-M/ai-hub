package com.mordan.aihub.fitness.domain.ai;

import java.util.List;

/**
 * AI 返回的周计划结果（用于 Jackson 反序列化）
 *
 * @author fitness
 */
public record WeeklyPlanResult(
    /** 计划标题 */
    String title,
    /** 整体计划说明（200字以内） */
    String summary,
    /** 每日训练列表 */
    List<DayResult> days
) {}
