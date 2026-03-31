package com.mordan.aihub.lowcode.web.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 版本VO
 */
@Data
@Builder
public class VersionVO {

    /** ID */
    private Long id;

    /** 应用ID */
    private Long appId;

    /** 预览地址（即部署地址） */
    private String previewUrl;

    /** 下载地址 */
    private String downloadUrl;

    /** 项目文件摘要 */
    private String projectSummary;

    /** 文件大小 */
    private Long fileSize;

    /** 创建时间戳 */
    private Long createdAt;

    /** 更新时间戳 */
    private Long updatedAt;
}
