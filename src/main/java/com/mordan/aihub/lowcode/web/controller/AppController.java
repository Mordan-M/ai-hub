package com.mordan.aihub.lowcode.web.controller;

import com.mordan.aihub.auth.service.UserService;
import com.mordan.aihub.common.utils.ResultUtils;
import com.mordan.aihub.common.vo.BaseResponse;
import com.mordan.aihub.lowcode.domain.service.ApplicationService;
import com.mordan.aihub.lowcode.web.request.CreateAppRequest;
import com.mordan.aihub.lowcode.web.request.UpdateAppRequest;
import com.mordan.aihub.lowcode.web.vo.AppVO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 应用管理Controller
 */
@RestController
@RequestMapping("/api/v1/lowcode/apps")
public class AppController {

    @Resource
    private UserService userService;

    @Resource
    private ApplicationService applicationService;

    /**
     * 创建应用
     */
    @PostMapping
    public BaseResponse<AppVO> createApp(@Valid @RequestBody CreateAppRequest request) {
        Long userId = userService.getCurrentUserId();
        AppVO appVO = applicationService.createApp(userId, request);
        return ResultUtils.success(appVO);
    }

    /**
     * 获取当前用户所有活跃应用列表
     */
    @GetMapping
    public BaseResponse<List<AppVO>> listApps() {
        Long userId = userService.getCurrentUserId();
        List<AppVO> apps = applicationService.listApps(userId);
        return ResultUtils.success(apps);
    }

    /**
     * 获取应用详情
     */
    @GetMapping("/{appId}")
    public BaseResponse<AppVO> getAppDetail(@PathVariable Long appId) {
        Long userId = userService.getCurrentUserId();
        AppVO appVO = applicationService.getAppDetail(userId, appId);
        return ResultUtils.success(appVO);
    }

    /**
     * 更新应用信息
     */
    @PutMapping("/{appId}")
    public BaseResponse<AppVO> updateApp(
            @PathVariable Long appId,
            @Valid @RequestBody UpdateAppRequest request) {
        Long userId = userService.getCurrentUserId();
        AppVO appVO = applicationService.updateApp(userId, appId, request);
        return ResultUtils.success(appVO);
    }

    /**
     * 删除应用（逻辑删除）
     */
    @DeleteMapping("/{appId}")
    public BaseResponse<Void> deleteApp(@PathVariable Long appId) {
        Long userId = userService.getCurrentUserId();
        applicationService.deleteApp(userId, appId);
        return ResultUtils.success(null);
    }
}
