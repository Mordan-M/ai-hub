package com.mordan.aihub.lowcode.web.vo;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Builder;
import lombok.Data;

/**
 * 版本VO
 */
@Data
@Builder
public class GenerateRecordVO {

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

    /** 代码文件存储路径 */
    private String codeStoragePath;

    /** 项目文件存储前缀（本次构建随机生成，用于隔离不同构建） */
    private String filePrefix;

}
