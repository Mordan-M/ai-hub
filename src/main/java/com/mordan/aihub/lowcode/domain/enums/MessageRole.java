package com.mordan.aihub.lowcode.domain.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 对话消息角色枚举
 */
@Getter
public enum MessageRole implements IEnum<Integer> {

    USER("用户"),
    ASSISTANT("助手"),
    TOOL("工具"),
    SYSTEM("系统");

    @JsonValue
    private final String desc;

    MessageRole(String desc) {
        this.desc = desc;
    }

    @Override
    public Integer getValue() {
        return this.ordinal();
    }
}
