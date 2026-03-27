package com.mordan.aihub.lowcode.web.vo;

import com.mordan.aihub.lowcode.domain.enums.VersionDeployStatus;
import lombok.Builder;
import lombok.Data;

/**
 * 版本VO
 */
@Data
@Builder
public class VersionVO {

    /** 版本ID */
    private Long id;

    /** 应用ID */
    private Long appId;

    /** 版本号 */
    private Integer versionNumber;

    /** 预览地址 */
    private String previewUrl;

    /** 下载地址 */
    private String downloadUrl;

    /** 部署状态 */
    private VersionDeployStatus deployStatus;

    /** 部署访问地址 */
    private String deployUrl;

    /** 创建时间戳 */
    private Long createdAt;
}
