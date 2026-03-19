package com.mordan.aihub.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mordan.aihub.fitness.domain.entity.DailyExerciseItem;
import com.mordan.aihub.fitness.domain.entity.DailyTraining;
import com.mordan.aihub.fitness.domain.entity.UserPreference;
import com.mordan.aihub.fitness.domain.entity.WeeklyPlan;
import com.mordan.aihub.fitness.domain.vo.DailyTrainingVO;
import com.mordan.aihub.fitness.domain.vo.ExerciseItemVO;
import com.mordan.aihub.fitness.domain.vo.UserPreferenceRequest;
import com.mordan.aihub.fitness.domain.vo.WeeklyPlanDetailVO;
import com.mordan.aihub.fitness.mapper.DailyExerciseItemMapper;
import com.mordan.aihub.fitness.mapper.DailyTrainingMapper;
import com.mordan.aihub.fitness.mapper.UserPreferenceMapper;
import com.mordan.aihub.fitness.mapper.WeeklyPlanMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 健身计划服务
 *
 * @author fitness
 */
@Slf4j
@Service
public class FitnessPlanService {

    @Resource
    private UserPreferenceMapper userPreferenceMapper;

    @Resource
    private WeeklyPlanMapper weeklyPlanMapper;

    @Resource
    private DailyTrainingMapper dailyTrainingMapper;

    @Resource
    private DailyExerciseItemMapper dailyExerciseItemMapper;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private FitnessPlanAsyncService fitnessPlanAsyncService;

    /**
     * 根据用户偏好生成健身计划（异步）
     * @param userId 用户ID
     * @param request 用户偏好请求
     * @return 新建计划ID
     */
    @Transactional
    public Long generatePlan(Long userId, UserPreferenceRequest request) {
        // 1. 保存用户偏好
        UserPreference preference = UserPreference.builder()
                .userId(userId)
                .experienceLevel(request.getExperienceLevel())
                .goal(request.getGoal())
                .focusMuscles(convertFocusMusclesToJson(request.getFocusMuscles()))
                .equipment(request.getEquipment())
                .sessionDuration(request.getSessionDuration())
                .trainingDaysPerWeek(request.getTrainingDaysPerWeek())
                .trainingStyle(request.getTrainingStyle())
                .injuryNotes(request.getInjuryNotes())
                .build();
        userPreferenceMapper.insert(preference);

        // 2. 创建周计划记录，状态为 generating
        LocalDate weekStart = getStartOfCurrentWeek();
        WeeklyPlan plan = WeeklyPlan.builder()
                .userId(userId)
                .preferenceId(preference.getId())
                .title("生成中...")
                .weekStartDate(weekStart)
                .status(WeeklyPlan.STATUS_GENERATING)
                .build();
        weeklyPlanMapper.insert(plan);

        // 3. 异步执行生成 - 独立异步 Service 确保 @Async 生效
        fitnessPlanAsyncService.asyncGeneratePlan(plan.getId(), preference);

        // 4. 立即返回 planId，前端轮询
        return plan.getId();
    }

    /**
     * 获取计划详情
     * @param planId 计划ID
     * @param currentUserId 当前登录用户ID，用于权限校验
     */
    public WeeklyPlanDetailVO getPlanDetail(Long planId, Long currentUserId) {
        WeeklyPlan plan = weeklyPlanMapper.selectById(planId);
        if (plan == null) {
            return null;
        }
        // 校验当前计划是否归属当前用户
        if (!plan.getUserId().equals(currentUserId)) {
            throw new RuntimeException("无权限访问该计划");
        }

        WeeklyPlanDetailVO vo = new WeeklyPlanDetailVO();
        vo.setPlanId(plan.getId());
        vo.setTitle(plan.getTitle());
        vo.setSummary(plan.getSummary());
        vo.setWeekStartDate(plan.getWeekStartDate());
        vo.setStatus(plan.getStatus());

        // 如果还在生成中，只返回基本信息
        if (!WeeklyPlan.STATUS_DONE.equals(plan.getStatus())) {
            vo.setDays(new ArrayList<>());
            return vo;
        }

        // 查询每日训练
        List<DailyTraining> dailyTrainings = dailyTrainingMapper.selectList(
                new LambdaQueryWrapper<DailyTraining>()
                        .eq(DailyTraining::getPlanId, planId)
                        .orderByAsc(DailyTraining::getDayOfWeek)
        );

        List<DailyTrainingVO> dayVOs = new ArrayList<>();
        for (DailyTraining training : dailyTrainings) {
            DailyTrainingVO dayVo = new DailyTrainingVO();
            dayVo.setDayOfWeek(training.getDayOfWeek());
            dayVo.setIsRestDay(training.getIsRestDay() == 1);
            dayVo.setFocusMuscleGroup(training.getFocusMuscleGroup());
            dayVo.setWarmUpNotes(training.getWarmUpNotes());
            dayVo.setCoolDownNotes(training.getCoolDownNotes());

            // 查询动作列表
            List<DailyExerciseItem> items = dailyExerciseItemMapper.selectList(
                    new LambdaQueryWrapper<DailyExerciseItem>()
                            .eq(DailyExerciseItem::getDailyTrainingId, training.getId())
                            .orderByAsc(DailyExerciseItem::getSortOrder)
            );

            List<ExerciseItemVO> itemVOs = getExerciseItemVOS(items);

            dayVo.setExercises(itemVOs);
            dayVOs.add(dayVo);
        }

        vo.setDays(dayVOs);
        return vo;
    }

    private static @NonNull List<ExerciseItemVO> getExerciseItemVOS(List<DailyExerciseItem> items) {
        List<ExerciseItemVO> itemVOs = new ArrayList<>();
        for (DailyExerciseItem item : items) {
            ExerciseItemVO itemVo = new ExerciseItemVO();
            itemVo.setNameZh(item.getNameZh());
            itemVo.setNameEn(item.getNameEn());
            itemVo.setSets(item.getSets());
            itemVo.setReps(item.getReps());
            itemVo.setRestSeconds(item.getRestSeconds());
            itemVo.setCoachNotes(item.getCoachNotes());
            itemVo.setBilibiliUrl(item.getBilibiliUrl());
            itemVOs.add(itemVo);
        }
        return itemVOs;
    }

    /**
     * 获取用户最新计划
     */
    public WeeklyPlanDetailVO getLatestPlan(Long userId) {
        WeeklyPlan latest = weeklyPlanMapper.selectOne(
                new LambdaQueryWrapper<WeeklyPlan>()
                        .eq(WeeklyPlan::getUserId, userId)
                        .orderByDesc(WeeklyPlan::getCreatedAt)
                        .last("LIMIT 1")
        );
        if (latest == null) {
            return null;
        }
        return getPlanDetail(latest.getId(), userId);
    }

    /**
     * 重新生成计划
     * @param planId 原计划ID
     * @param currentUserId 当前登录用户ID，用于权限校验
     */
    @Transactional
    public Long regeneratePlan(Long planId, Long currentUserId) {
        WeeklyPlan oldPlan = weeklyPlanMapper.selectById(planId);
        if (oldPlan == null) {
            return null;
        }
        // 校验原计划是否归属当前用户
        if (!oldPlan.getUserId().equals(currentUserId)) {
            throw new RuntimeException("无权限访问该计划");
        }
        UserPreference preference = userPreferenceMapper.selectById(oldPlan.getPreferenceId());
        if (preference == null) {
            return null;
        }

        // 转换为 request 重新生成
        UserPreferenceRequest request = convertToRequest(preference);
        return generatePlan(currentUserId, request);
    }

    private LocalDate getStartOfCurrentWeek() {
        LocalDate today = LocalDate.now();
        return today.minusDays(today.getDayOfWeek().getValue() - 1);
    }

    private String convertFocusMusclesToJson(List<String> focusMuscles) {
        try {
            return objectMapper.writeValueAsString(focusMuscles);
        } catch (JsonProcessingException e) {
            log.warn("转换 focusMuscles 到 JSON 失败", e);
            return "[]";
        }
    }

    private UserPreferenceRequest convertToRequest(UserPreference preference) {
        UserPreferenceRequest request = new UserPreferenceRequest();
        request.setExperienceLevel(preference.getExperienceLevel());
        request.setGoal(preference.getGoal());
        try {
            List<String> focusMuscles = objectMapper.readValue(preference.getFocusMuscles(), List.class);
            request.setFocusMuscles(focusMuscles);
        } catch (JsonProcessingException e) {
            request.setFocusMuscles(new ArrayList<>());
        }
        request.setEquipment(preference.getEquipment());
        request.setSessionDuration(preference.getSessionDuration());
        request.setTrainingDaysPerWeek(preference.getTrainingDaysPerWeek());
        request.setTrainingStyle(preference.getTrainingStyle());
        request.setInjuryNotes(preference.getInjuryNotes());
        return request;
    }
}
