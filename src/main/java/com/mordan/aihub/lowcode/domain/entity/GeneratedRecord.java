package com.mordan.aihub.lowcode.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 生成代码实体（每个应用对应一条记录）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("generated_record")
public class GeneratedRecord {

    /** ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属应用ID */
    private Long appId;

    /** 最后一次生成此应用代码的任务ID */
    private Long taskId;

    /** 代码文件存储路径 */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private String codeStoragePath;

    /** 预览/部署地址 */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private String previewUrl;

    /** 下载地址 */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private String downloadUrl;

    /** 代码包大小（字节） */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Long fileSize;

    /** 生成时的完整提示词快照 */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private String promptSnapshot;

    /** 项目文件摘要（大模型返回，简要说明每个文件作用） */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private String projectSummary;

    /** 项目文件存储前缀（本次构建随机生成，用于隔离不同构建） */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private String filePrefix;

    /** 部署后访问 URL（用户部署时写入） */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private String deployUrl;

    /** 创建时间戳（毫秒） */
    @TableField(fill = FieldFill.INSERT)
    private Long createdAt;

    /** 更新时间戳（毫秒） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedAt;
}
