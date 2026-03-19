package com.mordan.aihub.fitness.domain.ai;

import lombok.Data;

import java.util.List;

/**
 * AI 返回的周计划结果（用于 Jackson 反序列化）
 *
 * @author fitness
 */
@Data
public class WeeklyPlanResult {

    /** 计划标题 */
    private String title;
    /** 整体计划说明（200字以内） */
    private String summary;
    /** 每日训练列表 */
    private List<DayResult> days;
}
