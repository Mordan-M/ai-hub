package com.mordan.aihub.lowcode.web.controller;

import com.mordan.aihub.lowcode.infrastructure.storage.FileStorageService;
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
 */
@Slf4j
@RestController
@RequestMapping("/preview")
public class PreviewController extends BasePreviewController {

    @Resource
    private FileStorageService fileStorageService;

    /**
     * 访问 /preview/{appId}/{versionId} 时自动跳转到 index.html
     */
    @GetMapping("/{appId}")
    public void redirectToIndex(@PathVariable String appId,
                                HttpServletResponse response) throws IOException {
        String url = "/preview/lowcode-output-" + appId + "/dist/index.html";
        response.sendRedirect(url);
    }

}
