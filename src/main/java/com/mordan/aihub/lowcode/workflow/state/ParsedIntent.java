package com.mordan.aihub.lowcode.workflow.state;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 解析后的结构化意图 POJO
 * 存储用户需求解析后的结构化信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedIntent implements Serializable {

    @Serial
    private static final long serialVersionUID = 7337403234831684988L;

    /**
     * 页面列表
     */
    private List<PageInfo> pages;

    /**
     * 整体风格描述
     */
    private String style;

    /**
     * 主色调描述
     */
    private String colorScheme;

    /**
     * 是否需要对接后端 API
     */
    private Boolean hasApiIntegration;

    /**
     * API 接口列表
     */
    private List<ApiEndpointInfo> apiEndpoints;

    /**
     * 图片需求列表
     */
    private List<ImageNeedInfo> imageNeeds;

    /**
     * 是否为迭代修改
     */
    private Boolean isIteration;

    /**
     * 本次迭代修改重点（isIteration=true 时填写）
     */
    private String iterationFocus;

    /**
     * 页面信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageInfo implements Serializable {

        @Serial
        private static final long serialVersionUID = 3990436013366113161L;

        /** 页面名 */
        private String name;
        /** 路由路径 */
        private String route;
        /** 页面包含的组件列表 */
        private List<String> components;
        /** 页面功能简述 */
        private String description;
    }

    /**
     * API 端点信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiEndpointInfo implements Serializable {

        @Serial
        private static final long serialVersionUID = 5539629916309174819L;

        /** 接口路径 */
        private String path;
        /** 请求方法 */
        private String method;
        /** 用途描述 */
        private String usage;
    }

    /**
     * 图片需求信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageNeedInfo implements Serializable {

        @Serial
        private static final long serialVersionUID = -8174031127999208116L;

        /** 位置描述 */
        private String location;
        /** 搜索关键词（英文） */
        private String keyword;
        /** 图片宽度 */
        private Integer width;
        /** 图片高度 */
        private Integer height;
    }
}
