package com.mordan.aihub.lowcode.web.vo;

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

    /** 预览地址（即部署地址） */
    private String previewUrl;

    /** 下载地址 */
    private String downloadUrl;

    /** 创建时间戳 */
    private Long createdAt;
}
