package com.mordan.aihub.lowcode.web.controller;

import cn.hutool.core.io.FileUtil;
import com.mordan.aihub.auth.exception.BusinessException;
import com.mordan.aihub.auth.exception.ThrowUtils;
import com.mordan.aihub.auth.service.UserService;
import com.mordan.aihub.common.utils.ResultUtils;
import com.mordan.aihub.common.vo.BaseResponse;
import com.mordan.aihub.common.vo.ErrorCode;
import com.mordan.aihub.lowcode.constant.AppConstant;
import com.mordan.aihub.lowcode.domain.entity.Application;
import com.mordan.aihub.lowcode.domain.service.ApplicationService;
import com.mordan.aihub.lowcode.domain.service.GeneratedRecordService;
import com.mordan.aihub.lowcode.web.request.CreateAppRequest;
import com.mordan.aihub.lowcode.web.request.UpdateAppRequest;
import com.mordan.aihub.lowcode.web.vo.AppVO;
import com.mordan.aihub.lowcode.web.vo.GenerateRecordVO;
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

import java.io.File;
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

    @Resource
    private GeneratedRecordService generatedRecordService;

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

    @PostMapping("/deploy/{appId}")
    public BaseResponse<String> downloadAppCode(@PathVariable Long appId) {
        // 1. 基础校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");

        // 2. 查询应用信息
        Application app = applicationService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        GenerateRecordVO generatedRecord = generatedRecordService.getGeneratedRecord(appId);
        ThrowUtils.throwIf(generatedRecord == null, ErrorCode.NOT_FOUND_ERROR, "应用未生成代码");

        // 3. 权限校验：只有应用创建者可以部署代码
        Long userId = userService.getCurrentUserId();
        if (!app.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限部署");
        }

        // 4. 构建应用代码目录路径（生成目录，非部署目录）
        String sourceDirPath = generatedRecord.getCodeStoragePath();

        // 5. 检查代码目录是否存在
        File sourceDir = new File(sourceDirPath);
        ThrowUtils.throwIf(!sourceDir.exists() || !sourceDir.isDirectory(),
                ErrorCode.NOT_FOUND_ERROR, "应用代码不存在，请先生成代码");

        // 6. 复制文件到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + AppConstant.CODE_OUTPUT_PREFIX + generatedRecord.getFilePrefix();
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用部署失败：" + e.getMessage());
        }

        // 7. 部署链接写入生成记录中
        String deployUrl = "/lowcode/deploy/" + AppConstant.CODE_OUTPUT_PREFIX + generatedRecord.getFilePrefix() + "/dist/index.html";
        // 使用 lambda 更新
        generatedRecordService.lambdaUpdate()
                .eq(com.mordan.aihub.lowcode.domain.entity.GeneratedRecord::getAppId, appId)
                .set(com.mordan.aihub.lowcode.domain.entity.GeneratedRecord::getDeployUrl, deployUrl)
                .update();

        return ResultUtils.success(deployUrl);
    }

    /**
     * 获取应用生成记录信息（包含预览地址和部署地址）
     */
    @GetMapping("/{appId}/generated-info")
    public BaseResponse<GenerateRecordVO> getGeneratedInfo(@PathVariable Long appId) {
        // 1. 基础校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");

        // 2. 查询应用信息
        Application app = applicationService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        // 3. 权限校验
        Long userId = userService.getCurrentUserId();
        if (!app.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问");
        }

        // 4. 查询生成记录获取预览地址和部署地址
        GenerateRecordVO generatedRecord = generatedRecordService.getGeneratedRecord(appId);

        return ResultUtils.success(generatedRecord);
    }
}
