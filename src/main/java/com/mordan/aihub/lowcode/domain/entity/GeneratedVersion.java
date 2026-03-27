package com.mordan.aihub.lowcode.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.mordan.aihub.lowcode.domain.enums.VersionDeployStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 生成代码版本实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("generated_version")
public class GeneratedVersion {

    /** 版本ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属应用ID */
    private Long appId;

    /** 生成此版本的任务ID */
    private Long taskId;

    /** 版本号（自增） */
    private Integer versionNumber;

    /** 代码文件存储路径 */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private String codeStoragePath;

    /** 沙箱预览地址 */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private String previewUrl;

    /** 下载地址 */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private String downloadUrl;

    /** 代码包大小（字节） */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Long fileSize;

    /** 代码校验详情 */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private String validationResult;

    /** 生成此版本时的完整提示词快照 */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private String promptSnapshot;

    /** 部署状态 */
    @TableField(typeHandler = org.apache.ibatis.type.EnumTypeHandler.class)
    private VersionDeployStatus deployStatus;

    /** 部署路径随机标识符，全局唯一 */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private String deploySlug;

    /** 完整部署访问地址 */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private String deployUrl;

    /** 部署时间戳（毫秒） */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Long deployedAt;

    /** 下线时间戳（毫秒） */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Long undeployedAt;

    /** 创建时间戳（毫秒） */
    @TableField(fill = FieldFill.INSERT)
    private Long createdAt;
}
