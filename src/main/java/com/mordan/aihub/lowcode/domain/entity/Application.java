package com.mordan.aihub.lowcode.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.mordan.aihub.lowcode.domain.enums.AppStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 低代码应用实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("application")
public class Application {

    /** 应用ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户ID */
    private Long userId;

    /** 应用名称 */
    private String name;

    /** 应用描述 */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private String description;

    /** 应用状态 */
    private AppStatus status;

    /** 预览缩略图地址 */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private String thumbnailUrl;

    /** 创建时间戳（毫秒） */
    @TableField(fill = FieldFill.INSERT)
    private Long createdAt;

    /** 更新时间戳（毫秒） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedAt;
}
