package com.mordan.aihub.lowcode.web.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 部署结果VO
 */
@Data
@Builder
public class DeployResultVO {

    /** 部署标识 */
    private String deploySlug;

    /** 部署访问地址 */
    private String deployUrl;

    /** 部署时间戳 */
    private Long deployedAt;
}
