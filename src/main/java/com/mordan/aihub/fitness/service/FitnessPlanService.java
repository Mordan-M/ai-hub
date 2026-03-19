package com.mordan.aihub.fitness.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mordan.aihub.fitness.domain.entity.WeeklyPlan;
import com.mordan.aihub.fitness.domain.vo.UserPreferenceRequest;
import com.mordan.aihub.fitness.domain.vo.WeeklyPlanDetailVO;

/**
 * 健身计划服务接口
 *
 * @author fitness
 */
public interface FitnessPlanService extends IService<WeeklyPlan> {

    /**
     * 根据用户偏好生成健身计划（异步）
     * @param userId 用户ID
     * @param request 用户偏好请求
     * @return 新建计划ID
     */
    Long generatePlan(Long userId, UserPreferenceRequest request);

    /**
     * 获取计划详情
     * @param planId 计划ID
     * @param currentUserId 当前登录用户ID，用于权限校验
     * @return 计划详情VO
     */
    WeeklyPlanDetailVO getPlanDetail(Long planId, Long currentUserId);

    /**
     * 获取用户最新计划
     * @param userId 用户ID
     * @return 最新计划详情
     */
    WeeklyPlanDetailVO getLatestPlan(Long userId);

    /**
     * 重新生成计划
     * @param planId 原计划ID
     * @param currentUserId 当前登录用户ID，用于权限校验
     * @return 新计划ID
     */
    Long regeneratePlan(Long planId, Long currentUserId);
}
