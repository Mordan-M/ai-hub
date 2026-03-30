package com.mordan.aihub.lowcode.web.controller;

import com.mordan.aihub.lowcode.infrastructure.storage.FileStorageService;
import com.mordan.aihub.lowcode.mapper.GeneratedVersionMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * 沙箱预览Controller
 * 无需鉴权，直接提供静态文件服务
 * 预览路径即为部署路径
 */
@Slf4j
@RestController
@RequestMapping("/lowcode/preview")
public class PreviewController {

    @Resource
    private FileStorageService fileStorageService;
    @Resource
    private GeneratedVersionMapper generatedVersionMapper;

    /**
     * 访问 /preview/{appId} 时自动跳转到 index.html
     */
    @GetMapping("/{appId}")
    public void redirectToIndex(@PathVariable String appId,
                                HttpServletResponse response) throws IOException {
        String url = "/lowcode/preview/lowcode-output-" + appId + "/dist/index.html";
        response.sendRedirect(url);
    }
//    /**
//     * 下载版本 ZIP 包
//     * 路径: /preview/{versionId}/download
//     */
//    @GetMapping("/{versionId}/download")
//    public ResponseEntity<Resource> downloadVersion(@PathVariable Long versionId) {
//        GeneratedVersion version = generatedVersionMapper.selectById(versionId);
//        if (version == null || version.getCodeStoragePath() == null) {
//            return ResponseEntity.notFound().build();
//        }
//
//        try {
//            ByteArrayResource zipResource = fileStorageService.packageAsZip(version.getCodeStoragePath());
//            String filename = String.format("app-v%d.zip", version.getVersionNumber());
//
//            return ResponseEntity.ok()
//                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
//                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
//                    .contentLength(zipResource.contentLength())
//                    .body(zipResource);
//        } catch (IOException e) {
//            throw new RuntimeException("打包失败：" + e.getMessage(), e);
//        }
//    }

}
