package com.mordan.aihub.lowcode.web.controller;

import com.mordan.aihub.auth.service.UserService;
import com.mordan.aihub.common.vo.BaseResponse;
import com.mordan.aihub.common.utils.ResultUtils;
import com.mordan.aihub.lowcode.domain.service.GeneratedVersionService;
import com.mordan.aihub.lowcode.web.vo.DeployResultVO;
import com.mordan.aihub.lowcode.web.vo.DeployStatusVO;
import com.mordan.aihub.lowcode.web.vo.TaskVO;
import com.mordan.aihub.lowcode.web.vo.VersionVO;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 版本管理Controller
 */
@RestController
@RequestMapping("/api/v1/lowcode/apps/{appId}/versions")
public class VersionController {

    @Resource
    private UserService userService;

    @Resource
    private GeneratedVersionService generatedVersionService;

    /**
     * 获取应用版本列表
     */
    @GetMapping
    public BaseResponse<List<VersionVO>> listVersions(@PathVariable Long appId) {
        Long userId = userService.getCurrentUserId();
        List<VersionVO> versions = generatedVersionService.listVersions(userId, appId);
        return ResultUtils.success(versions);
    }

    /**
     * 获取版本详情
     */
    @GetMapping("/{versionId}")
    public BaseResponse<VersionVO> getVersionDetail(
            @PathVariable Long appId,
            @PathVariable Long versionId) {
        Long userId = userService.getCurrentUserId();
        VersionVO version = generatedVersionService.getVersionDetail(userId, appId, versionId);
        return ResultUtils.success(version);
    }

    /**
     * 回滚到指定版本，重新提交生成任务
     */
    @PostMapping("/{versionId}/rollback")
    public BaseResponse<TaskVO> rollbackToVersion(
            @PathVariable Long appId,
            @PathVariable Long versionId) {
        Long userId = userService.getCurrentUserId();
        TaskVO taskVO = generatedVersionService.rollbackToVersion(userId, appId, versionId);
        return ResultUtils.success(taskVO);
    }

    /**
     * 下载版本ZIP包
     */
    @GetMapping("/{versionId}/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadVersion(
            @PathVariable Long appId,
            @PathVariable Long versionId) {
        Long userId = userService.getCurrentUserId();
        return generatedVersionService.downloadVersion(userId, appId, versionId);
    }

    /**
     * 部署版本
     */
    @PostMapping("/{versionId}/deploy")
    public BaseResponse<DeployResultVO> deployVersion(
            @PathVariable Long appId,
            @PathVariable Long versionId) {
        Long userId = userService.getCurrentUserId();
        DeployResultVO result = generatedVersionService.deployVersion(userId, appId, versionId);
        return ResultUtils.success(result);
    }

    /**
     * 下线版本
     */
    @DeleteMapping("/{versionId}/deploy")
    public BaseResponse<Void> undeployVersion(
            @PathVariable Long appId,
            @PathVariable Long versionId) {
        Long userId = userService.getCurrentUserId();
        generatedVersionService.undeployVersion(userId, appId, versionId);
        return ResultUtils.success(null);
    }

    /**
     * 获取部署状态
     */
    @GetMapping("/{versionId}/deploy")
    public BaseResponse<DeployStatusVO> getDeployStatus(
            @PathVariable Long appId,
            @PathVariable Long versionId) {
        Long userId = userService.getCurrentUserId();
        DeployStatusVO status = generatedVersionService.getDeployStatus(userId, appId, versionId);
        return ResultUtils.success(status);
    }
}
