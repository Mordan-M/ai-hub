package com.mordan.aihub.lowcode.domain.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 应用状态枚举
 */
@Getter
public enum AppStatus implements IEnum<String> {

    ACTIVE("ACTIVE", "活跃"),
    ARCHIVED("ARCHIVED", "已归档");

    @JsonValue
    private final String value;
    private final String desc;

    AppStatus(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @Override
    public String getValue() {
        return value;
    }
}
