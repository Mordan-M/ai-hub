package com.mordan.aihub.lowcode.web.vo;

import com.mordan.aihub.lowcode.domain.enums.AppStatus;
import lombok.Builder;
import lombok.Data;

/**
 * 应用详情VO
 */
@Data
@Builder
public class AppVO {

    /** 应用ID */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 应用名称 */
    private String name;

    /** 应用描述 */
    private String description;

    /** 应用状态 */
    private AppStatus status;

    /** 预览缩略图地址 */
    private String thumbnailUrl;

    /** 创建时间戳 */
    private Long createdAt;

    /** 更新时间戳 */
    private Long updatedAt;
}
