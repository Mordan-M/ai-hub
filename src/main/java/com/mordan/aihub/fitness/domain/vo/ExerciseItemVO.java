package com.mordan.aihub.fitness.domain.vo;

/**
 * 健身动作详情 VO
 *
 * @author fitness
 */
public record ExerciseItemVO(
    /** 动作中文名 */
    String nameZh,
    /** 动作英文名 */
    String nameEn,
    /** 组数 */
    Integer sets,
    /** 次数或时长，如：10-12 或 30s */
    String reps,
    /** 组间休息秒数 */
    Integer restSeconds,
    /** AI生成的动作要点提示 */
    String coachNotes,
    /** B站视频搜索跳转链接 */
    String bilibiliUrl
) {}
