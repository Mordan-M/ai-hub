package com.mordan.aihub.lowcode.domain.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 对话消息角色枚举
 */
@Getter
public enum MessageRole implements IEnum<String> {

    USER("USER", "用户"),
    ASSISTANT("ASSISTANT", "助手"),
    TOOL("TOOL", "工具"),
    SYSTEM("SYSTEM", "系统");

    @JsonValue
    private final String value;
    private final String desc;

    MessageRole(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @Override
    public String getValue() {
        return value;
    }
}
