package com.mordan.aihub.lowcode.web.controller;

import com.mordan.aihub.auth.exception.BusinessException;
import com.mordan.aihub.auth.exception.ThrowUtils;
import com.mordan.aihub.auth.service.UserService;
import com.mordan.aihub.common.vo.ErrorCode;
import com.mordan.aihub.lowcode.domain.entity.Application;
import com.mordan.aihub.lowcode.domain.service.ApplicationService;
import com.mordan.aihub.lowcode.domain.service.GeneratedRecordService;
import com.mordan.aihub.lowcode.domain.service.ProjectDownloadService;
import com.mordan.aihub.lowcode.web.vo.GenerateRecordVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
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
    private UserService userService;

    @Resource
    private ApplicationService applicationService;

    @Resource
    private GeneratedRecordService generatedRecordService;

    @Resource
    private ProjectDownloadService projectDownloadService;

    /**
     * 访问 /preview/{appId} 时自动跳转到 index.html
     */
    @GetMapping("/{appId}")
    public void redirectToIndex(@PathVariable String appId,
                                HttpServletResponse response) throws IOException {

        Application app = applicationService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        GenerateRecordVO generatedRecord = generatedRecordService.getGeneratedRecord(Long.valueOf(appId));
        ThrowUtils.throwIf(generatedRecord == null, ErrorCode.NOT_FOUND_ERROR, "应用未生成代码");

//        String url = "/lowcode/preview/lowcode-output-" + appId + "/dist/index.html";
        response.sendRedirect(generatedRecord.getPreviewUrl());
    }


    @GetMapping("/download/{appId}")
    public void downloadAppCode(@PathVariable Long appId,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        // 1. 基础校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");

        // 2. 查询应用信息
        Application app = applicationService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        GenerateRecordVO generatedRecord = generatedRecordService.getGeneratedRecord(appId);
        ThrowUtils.throwIf(generatedRecord == null, ErrorCode.NOT_FOUND_ERROR, "应用未生成代码");

        // 3. 权限校验：只有应用创建者可以下载代码
        Long userId = userService.getCurrentUserId();
        if (!app.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限下载该应用代码");
        }

        // 4. 构建应用代码目录路径（生成目录，非部署目录）
        String sourceDirPath = generatedRecord.getCodeStoragePath();

        // 5. 检查代码目录是否存在
        File sourceDir = new File(sourceDirPath);
        ThrowUtils.throwIf(!sourceDir.exists() || !sourceDir.isDirectory(),
                ErrorCode.NOT_FOUND_ERROR, "应用代码不存在，请先生成代码");

        // 6. 生成下载文件名（不建议添加中文内容）
        String downloadFileName = sourceDir.getName();

        // 7. 调用通用下载服务
        projectDownloadService.downloadProjectAsZip(sourceDirPath, downloadFileName, response);
    }

}
