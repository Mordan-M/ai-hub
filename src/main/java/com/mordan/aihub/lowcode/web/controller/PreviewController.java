package com.mordan.aihub.lowcode.web.controller;

import com.mordan.aihub.lowcode.infrastructure.storage.FileStorageService;
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
import java.util.concurrent.TimeUnit;

/**
 * 沙箱预览Controller
 * 无需鉴权，直接提供静态文件服务
 */
@Slf4j
@RestController
@RequestMapping("/preview")
public class PreviewController extends BasePreviewController {

    @Resource
    private FileStorageService fileStorageService;

    /**
     * 预览版本的静态文件
     * 路径: /preview/{versionId}/**
     */
    @GetMapping("/{versionId}/**")
    public ResponseEntity<org.springframework.core.io.Resource> preview(
            @PathVariable Long versionId,
            HttpServletRequest request) {

        // 1. 提取相对路径，空或 "/" 时默认 index.html
        String prefix = "/preview/" + versionId;
        String relativePath = extractRelativePath(request, prefix);
        if (relativePath.isEmpty() || relativePath.equals("/")) {
            relativePath = "index.html";
        }

        // 2. 获取版本根目录
        Path versionRoot = fileStorageService.getVersionRoot(versionId);
        if (versionRoot == null || !Files.exists(versionRoot)) {
            return ResponseEntity.notFound().build();
        }

        // 3. 路径穿越防护
        Path targetFile = versionRoot.resolve(relativePath).normalize();
        if (!targetFile.startsWith(versionRoot)) {
            return ResponseEntity.badRequest().build();
        }

        // 4. 文件不存在时回退到 index.html（支持React Router）
        if (!Files.exists(targetFile)) {
            targetFile = versionRoot.resolve("index.html");
        }
        if (!Files.exists(targetFile)) {
            return ResponseEntity.notFound().build();
        }

        // 5. 如果是 index.html，需要修正其中的绝对路径前缀
        MediaType mediaType = resolveMediaType(relativePath);
        if ("index.html".equals(relativePath)) {
            try {
                String content = Files.readString(targetFile);
                // 将所有绝对路径 /xxx 替换为 /preview/{versionId}/xxx
                String basePath = "/preview/" + versionId;
                content = content.replaceAll("href=\"/", "href=\"" + basePath + "/");
                content = content.replaceAll("src=\"/", "src=\"" + basePath + "/");
                byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
                ByteArrayResource fileResource = new ByteArrayResource(bytes);
                return ResponseEntity.ok()
                        .contentType(mediaType)
                        .cacheControl(CacheControl.noCache())
                        .contentLength(bytes.length)
                        .body(fileResource);
            } catch (Exception e) {
                log.warn("Failed to process index.html", e);
            }
        }

        // 6. 返回原始文件
        org.springframework.core.io.Resource fileResource = new FileSystemResource(targetFile);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
                .body(fileResource);
    }
}
