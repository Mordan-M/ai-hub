package com.mordan.aihub.lowcode.workflow.state;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 意图检查结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentCheckResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1231456789012345678L;

    /**
     * 是否包含生成网站的意图
     */
    private Boolean hasIntent;

    /**
     * 原因说明
     */
    private String reason;
}
