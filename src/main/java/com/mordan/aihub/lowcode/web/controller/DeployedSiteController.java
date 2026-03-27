package com.mordan.aihub.lowcode.web.controller;

import com.mordan.aihub.lowcode.infrastructure.deploy.SlugRoutingTable;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * 已部署站点Controller
 * 无需鉴权，通过slug路由访问已部署的站点
 */
@Slf4j
@RestController
@RequestMapping("/site")
public class DeployedSiteController extends BasePreviewController {

    @Resource
    private SlugRoutingTable slugRoutingTable;

    /**
     * 服务已部署站点的静态文件
     * 路径: /site/{slug}/**
     */
    @GetMapping("/{slug}/**")
    public ResponseEntity<org.springframework.core.io.Resource> serveSite(
            @PathVariable String slug,
            HttpServletRequest request) {

        // 1. 查询路由表
        Path siteRoot = slugRoutingTable.resolve(slug);
        if (siteRoot == null || !Files.exists(siteRoot)) {
            return ResponseEntity.notFound().build();
        }

        // 2. 提取相对路径
        String prefix = "/site/" + slug;
        String relativePath = extractRelativePath(request, prefix);
        if (relativePath.isEmpty() || relativePath.equals("/")) {
            relativePath = "index.html";
        }

        // 3. 路径穿越防护
        Path targetFile = siteRoot.resolve(relativePath).normalize();
        if (!targetFile.startsWith(siteRoot)) {
            return ResponseEntity.badRequest().build();
        }

        // 4. 不存在回退到 index.html（支持React Router）
        if (!Files.exists(targetFile)) {
            targetFile = siteRoot.resolve("index.html");
        }
        if (!Files.exists(targetFile)) {
            return ResponseEntity.notFound().build();
        }

        // 5. 如果是 index.html，需要修正其中的绝对路径前缀
        MediaType mediaType = resolveMediaType(relativePath);
        if ("index.html".equals(relativePath)) {
            try {
                String content = Files.readString(targetFile);
                // 将所有绝对路径 /xxx 替换为 /site/{slug}/xxx
                String basePath = "/site/" + slug;
                content = content.replaceAll("href=\"/", "href=\"" + basePath + "/");
                content = content.replaceAll("src=\"/", "src=\"" + basePath + "/");
                byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
                ByteArrayResource fileResource = new ByteArrayResource(bytes);
                return ResponseEntity.ok()
                        .contentType(mediaType)
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(1)))
                        .contentLength(bytes.length)
                        .body(fileResource);
            } catch (Exception e) {
                log.warn("Failed to process index.html", e);
            }
        }

        // 6. 返回文件，部署站点缓存时间更长
        org.springframework.core.io.Resource fileResource = new FileSystemResource(targetFile);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)))
                .body(fileResource);
    }
}
