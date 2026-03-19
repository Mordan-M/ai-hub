package com.mordan.aihub.fitness.domain.vo;

import lombok.Data;

/**
 * 健身动作详情 VO
 *
 * @author fitness
 */
@Data
public class ExerciseItemVO {

    /** 动作中文名 */
    private String nameZh;
    /** 动作英文名 */
    private String nameEn;
    /** 组数 */
    private Integer sets;
    /** 次数或时长，如：10-12 或 30s */
    private String reps;
    /** 组间休息秒数 */
    private Integer restSeconds;
    /** AI生成的动作要点提示 */
    private String coachNotes;
    /** B站视频搜索跳转链接 */
    private String bilibiliUrl;
}
