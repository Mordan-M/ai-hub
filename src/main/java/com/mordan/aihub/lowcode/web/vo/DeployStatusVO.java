package com.mordan.aihub.lowcode.web.vo;

import com.mordan.aihub.lowcode.domain.enums.VersionDeployStatus;
import lombok.Builder;
import lombok.Data;

/**
 * 部署状态VO
 */
@Data
@Builder
public class DeployStatusVO {

    /** 版本ID */
    private Long versionId;

    /** 部署状态 */
    private VersionDeployStatus deployStatus;

    /** 部署访问地址 */
    private String deployUrl;

    /** 部署时间戳 */
    private Long deployedAt;

    /** 下线时间戳 */
    private Long undeployedAt;
}
