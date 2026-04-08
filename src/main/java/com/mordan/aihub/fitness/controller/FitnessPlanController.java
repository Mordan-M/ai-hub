package com.mordan.aihub.fitness.controller;

import com.mordan.aihub.auth.service.UserService;
import com.mordan.aihub.common.utils.ResultUtils;
import com.mordan.aihub.common.vo.BaseResponse;
import com.mordan.aihub.fitness.domain.vo.UserPreferenceRequest;
import com.mordan.aihub.fitness.domain.vo.WeeklyPlanDetailVO;
import com.mordan.aihub.fitness.service.FitnessPlanService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * AI 健身计划助手控制器
 *
 * @author fitness
 */
@RestController
@RequestMapping("/ai-hub/fitness")
public class FitnessPlanController {

    @Resource
    private FitnessPlanService fitnessPlanService;

    @Resource
    private UserService userService;

    /**
     * 提交用户偏好并开始生成健身计划
     * 立即返回 planId，前端轮询获取结果
     */
    @PostMapping("/preferences")
    public BaseResponse<Long> submitPreferences(
            @Valid @RequestBody UserPreferenceRequest request) {
        Long userId = userService.getCurrentUserId();
        Long planId = fitnessPlanService.generatePlan(userId, request);
        return ResultUtils.success(planId);
    }

    /**
     * 获取计划详情
     * 如果 status = generating，前端需要继续轮询
     */
    @GetMapping("/plans/{planId}")
    public BaseResponse<WeeklyPlanDetailVO> getPlanDetail(@PathVariable Long planId) {
        Long userId = userService.getCurrentUserId();
        WeeklyPlanDetailVO planDetail = fitnessPlanService.getPlanDetail(planId, userId);
        return ResultUtils.success(planDetail);
    }

    /**
     * 获取用户最新计划
     */
    @GetMapping("/plans/latest")
    public BaseResponse<WeeklyPlanDetailVO> getLatestPlan() {
        Long userId = userService.getCurrentUserId();
        WeeklyPlanDetailVO latestPlan = fitnessPlanService.getLatestPlan(userId);
        return ResultUtils.success(latestPlan);
    }

    /**
     * 重新生成计划
     */
    @PostMapping("/plans/{planId}/regenerate")
    public BaseResponse<Long> regeneratePlan(@PathVariable Long planId) {
        Long userId = userService.getCurrentUserId();
        Long newPlanId = fitnessPlanService.regeneratePlan(planId, userId);
        return ResultUtils.success(newPlanId);
    }
}
