package com.mordan.aihub.lowcode.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;

/**
 * 预览和部署站点Controller的基类
 * 提供共用工具方法
 */
public abstract class BasePreviewController {

    /**
     * 从请求URI中提取相对路径
     * @param request HTTP请求
     * @param prefix 前缀路径（需要去掉的部分）
     * @return 相对路径
     */
    protected String extractRelativePath(HttpServletRequest request, String prefix) {
        String uri = request.getRequestURI();
        String path = uri.substring(prefix.length());
        return path.startsWith("/") ? path.substring(1) : path;
    }

    /**
     * 根据文件扩展名解析MediaType
     * @param path 文件路径
     * @return 对应的MediaType
     */
    protected MediaType resolveMediaType(String path) {
        if (path.endsWith(".html")) {
            return MediaType.TEXT_HTML;
        }
        if (path.endsWith(".css")) {
            return MediaType.valueOf("text/css");
        }
        if (path.endsWith(".js") || path.endsWith(".jsx") || path.endsWith(".mjs")) {
            return MediaType.valueOf("application/javascript");
        }
        if (path.endsWith(".ts") || path.endsWith(".tsx")) {
            return MediaType.valueOf("application/javascript");
        }
        if (path.endsWith(".json")) {
            return MediaType.APPLICATION_JSON;
        }
        if (path.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        if (path.endsWith(".svg")) {
            return MediaType.valueOf("image/svg+xml");
        }
        if (path.endsWith(".ico")) {
            return MediaType.valueOf("image/x-icon");
        }
        if (path.endsWith(".woff")) {
            return MediaType.valueOf("font/woff");
        }
        if (path.endsWith(".woff2")) {
            return MediaType.valueOf("font/woff2");
        }
        if (path.endsWith(".ttf")) {
            return MediaType.valueOf("font/ttf");
        }
        if (path.endsWith(".map")) {
            return MediaType.APPLICATION_JSON;
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
