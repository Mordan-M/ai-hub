package com.mordan.aihub.lowcode.domain.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 生成任务状态枚举
 */
@Getter
public enum TaskStatus implements IEnum<String> {

    PENDING("PENDING", "等待中"),
    RUNNING("RUNNING", "执行中"),
    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败");

    @JsonValue
    private final String value;
    private final String desc;

    TaskStatus(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @Override
    public String getValue() {
        return value;
    }
}
