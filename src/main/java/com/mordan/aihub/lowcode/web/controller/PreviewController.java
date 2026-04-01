package com.mordan.aihub.lowcode.web.controller;

import com.mordan.aihub.auth.exception.ThrowUtils;
import com.mordan.aihub.auth.service.UserService;
import com.mordan.aihub.common.vo.ErrorCode;
import com.mordan.aihub.lowcode.domain.entity.Application;
import com.mordan.aihub.lowcode.domain.service.ApplicationService;
import com.mordan.aihub.lowcode.domain.service.GeneratedRecordService;
import com.mordan.aihub.lowcode.web.vo.GenerateRecordVO;
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
    private UserService userService;

    @Resource
    private ApplicationService applicationService;

    @Resource
    private GeneratedRecordService generatedRecordService;

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

}
