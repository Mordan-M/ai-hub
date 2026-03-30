//package com.mordan.aihub.lowcode.web.controller;
//
//import com.mordan.aihub.auth.service.UserService;
//import com.mordan.aihub.common.vo.BaseResponse;
//import com.mordan.aihub.common.utils.ResultUtils;
//import com.mordan.aihub.lowcode.domain.service.GeneratedVersionService;
//import com.mordan.aihub.lowcode.web.vo.TaskVO;
//import com.mordan.aihub.lowcode.web.vo.VersionVO;
//import jakarta.annotation.Resource;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
///**
// * 版本管理Controller
// */
//@RestController
//@RequestMapping("/api/v1/lowcode/apps/{appId}/versions")
//public class VersionController {
//
//    @Resource
//    private UserService userService;
//
//    @Resource
//    private GeneratedVersionService generatedVersionService;
//
//    /**
//     * 获取应用版本列表
//     */
//    @GetMapping
//    public BaseResponse<List<VersionVO>> listVersions(@PathVariable Long appId) {
//        Long userId = userService.getCurrentUserId();
//        List<VersionVO> versions = generatedVersionService.listVersions(userId, appId);
//        return ResultUtils.success(versions);
//    }
//
//    /**
//     * 获取版本详情
//     */
//    @GetMapping("/{versionId}")
//    public BaseResponse<VersionVO> getVersionDetail(
//            @PathVariable Long appId,
//            @PathVariable Long versionId) {
//        Long userId = userService.getCurrentUserId();
//        VersionVO version = generatedVersionService.getVersionDetail(userId, appId, versionId);
//        return ResultUtils.success(version);
//    }
//
//    /**
//     * 回滚到指定版本，重新提交生成任务
//     */
//    @PostMapping("/{versionId}/rollback")
//    public BaseResponse<TaskVO> rollbackToVersion(
//            @PathVariable Long appId,
//            @PathVariable Long versionId) {
//        Long userId = userService.getCurrentUserId();
//        TaskVO taskVO = generatedVersionService.rollbackToVersion(userId, appId, versionId);
//        return ResultUtils.success(taskVO);
//    }
//}
