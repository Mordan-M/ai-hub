package com.mordan.aihub.lowcode.domain.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 版本部署状态枚举
 */
@Getter
public enum VersionDeployStatus implements IEnum<String> {

    NOT_DEPLOYED("NOT_DEPLOYED", "未部署"),
    DEPLOYED("DEPLOYED", "已部署"),
    UNDEPLOYED("UNDEPLOYED", "已下线");

    @JsonValue
    private final String value;
    private final String desc;

    VersionDeployStatus(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    @Override
    public String getValue() {
        return value;
    }
}
