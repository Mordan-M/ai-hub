package com.mordan.aihub.fitness.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mordan.aihub.fitness.domain.entity.DailyExerciseItem;
import com.mordan.aihub.fitness.domain.entity.DailyTraining;
import com.mordan.aihub.fitness.domain.entity.UserPreference;
import com.mordan.aihub.fitness.domain.entity.WeeklyPlan;
import com.mordan.aihub.fitness.domain.vo.DailyTrainingVO;
import com.mordan.aihub.fitness.domain.vo.ExerciseItemVO;
import com.mordan.aihub.fitness.domain.vo.UserPreferenceRequest;
import com.mordan.aihub.fitness.domain.vo.WeeklyPlanDetailVO;
import com.mordan.aihub.fitness.mapper.WeeklyPlanMapper;
import com.mordan.aihub.fitness.service.DailyExerciseItemService;
import com.mordan.aihub.fitness.service.DailyTrainingService;
import com.mordan.aihub.fitness.service.FitnessPlanAsyncService;
import com.mordan.aihub.fitness.service.FitnessPlanService;
import com.mordan.aihub.fitness.service.UserPreferenceService;
import com.mordan.aihub.fitness.service.WeeklyPlanService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 健身计划服务实现类
 *
 * @author fitness
 */
@Slf4j
@Service
public class FitnessPlanServiceImpl extends ServiceImpl<WeeklyPlanMapper, WeeklyPlan> implements FitnessPlanService {

    @Resource
    private UserPreferenceService userPreferenceService;

    @Resource
    private WeeklyPlanService weeklyPlanService;

    @Resource
    private DailyTrainingService dailyTrainingService;

    @Resource
    private DailyExerciseItemService dailyExerciseItemService;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private FitnessPlanAsyncService fitnessPlanAsyncService;

    @Override
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
        userPreferenceService.save(preference);

        // 2. 创建周计划记录，状态为 generating
        LocalDate weekStart = getStartOfCurrentWeek();
        WeeklyPlan plan = WeeklyPlan.builder()
                .userId(userId)
                .preferenceId(preference.getId())
                .title("生成中...")
                .weekStartDate(weekStart)
                .status(WeeklyPlan.STATUS_GENERATING)
                .build();
        weeklyPlanService.save(plan);

        // 3. 异步执行生成 - 独立异步 Service 确保 @Async 生效
        fitnessPlanAsyncService.asyncGeneratePlan(plan.getId(), preference);

        // 4. 立即返回 planId，前端轮询
        return plan.getId();
    }

    @Override
    public WeeklyPlanDetailVO getPlanDetail(Long planId, Long currentUserId) {
        WeeklyPlan plan = weeklyPlanService.getById(planId);
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
        List<DailyTraining> dailyTrainings = dailyTrainingService.lambdaQuery()
                .eq(DailyTraining::getPlanId, planId)
                .orderByAsc(DailyTraining::getDayOfWeek)
                .list();

        List<DailyTrainingVO> dayVOs = new ArrayList<>();
        for (DailyTraining training : dailyTrainings) {
            DailyTrainingVO dayVo = new DailyTrainingVO();
            dayVo.setDayOfWeek(training.getDayOfWeek());
            dayVo.setIsRestDay(training.getIsRestDay() == 1);
            dayVo.setFocusMuscleGroup(training.getFocusMuscleGroup());
            dayVo.setWarmUpNotes(training.getWarmUpNotes());
            dayVo.setCoolDownNotes(training.getCoolDownNotes());

            // 查询动作列表
            List<DailyExerciseItem> items = dailyExerciseItemService.lambdaQuery()
                    .eq(DailyExerciseItem::getDailyTrainingId, training.getId())
                    .orderByAsc(DailyExerciseItem::getSortOrder)
                    .list();

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

    @Override
    public WeeklyPlanDetailVO getLatestPlan(Long userId) {
        WeeklyPlan latest = weeklyPlanService.lambdaQuery()
                .eq(WeeklyPlan::getUserId, userId)
                .orderByDesc(WeeklyPlan::getCreatedAt)
                .last("LIMIT 1")
                .one();
        if (latest == null) {
            return null;
        }
        return getPlanDetail(latest.getId(), userId);
    }

    @Override
    @Transactional
    public Long regeneratePlan(Long planId, Long currentUserId) {
        WeeklyPlan oldPlan = weeklyPlanService.getById(planId);
        if (oldPlan == null) {
            return null;
        }
        // 校验原计划是否归属当前用户
        if (!oldPlan.getUserId().equals(currentUserId)) {
            throw new RuntimeException("无权限访问该计划");
        }
        UserPreference preference = userPreferenceService.getById(oldPlan.getPreferenceId());
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
