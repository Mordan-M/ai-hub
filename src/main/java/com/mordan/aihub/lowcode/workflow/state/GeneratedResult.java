package com.mordan.aihub.lowcode.workflow.state;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 生成的代码结果
 * 包含多个代码文件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 7891234560123456789L;

    /**
     * 项目文件摘要（结构化）
     */
    private ProjectSummary summary;

    /**
     * 项目文件摘要结构
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectSummary implements Serializable {
        @Serial
        private static final long serialVersionUID = 1234567890123456789L;

        /**
         * 项目名称
         */
        private String name;

        /**
         * 文件信息列表
         */
        private List<FileInfo> files;

        /**
         * 依赖信息（key: 依赖名称, value: 版本号）
         */
        private Map<String, String> dependencies;
    }

    /**
     * 单个文件信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1234567890123456789L;

        /**
         * 文件路径
         */
        private String path;

        /**
         * 文件作用描述
         */
        private String description;
    }
}
