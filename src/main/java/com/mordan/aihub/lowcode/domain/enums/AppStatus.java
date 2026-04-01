package com.mordan.aihub.lowcode.domain.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 应用状态枚举
 */
@Getter
public enum AppStatus implements IEnum<Integer> {

    ACTIVE("活跃"),
    ARCHIVED("已归档");

    @JsonValue
    private final String desc;

    AppStatus(String desc) {
        this.desc = desc;
    }

    @Override
    public Integer getValue() {
        return this.ordinal();
    }
}
