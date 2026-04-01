package com.mordan.aihub.lowcode.domain.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 生成任务状态枚举
 */
@Getter
public enum TaskStatus implements IEnum<Integer> {

    PENDING("等待中"),
    RUNNING("执行中"),
    SUCCESS("成功"),
    FAILED("失败");

    @JsonValue
    private final String desc;

    TaskStatus(String desc) {
        this.desc = desc;
    }

    @Override
    public Integer getValue() {
        return this.ordinal();
    }
}
