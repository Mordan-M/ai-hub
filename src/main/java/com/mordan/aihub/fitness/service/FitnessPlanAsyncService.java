package com.mordan.aihub.fitness.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mordan.aihub.fitness.ai.FitnessPlanAiService;
import com.mordan.aihub.fitness.domain.ai.DayResult;
import com.mordan.aihub.fitness.domain.ai.ExerciseResult;
import com.mordan.aihub.fitness.domain.ai.WeeklyPlanResult;
import com.mordan.aihub.fitness.domain.entity.DailyExerciseItem;
import com.mordan.aihub.fitness.domain.entity.DailyTraining;
import com.mordan.aihub.fitness.domain.entity.UserPreference;
import com.mordan.aihub.fitness.domain.entity.WeeklyPlan;
import com.mordan.aihub.fitness.domain.vo.UserPreferenceRequest;
import com.mordan.aihub.fitness.util.VideoLinkBuilder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 健身计划异步生成服务
 * 独立 Service，确保 @Async 代理正常生效
 *
 * @author fitness
 */
@Slf4j
@Service
public class FitnessPlanAsyncService {

    @Resource
    private WeeklyPlanService weeklyPlanService;

    @Resource
    private DailyTrainingService dailyTrainingService;

    @Resource
    private DailyExerciseItemService dailyExerciseItemService;

    @Resource
    private FitnessPlanAiService fitnessPlanAiService;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 异步生成健身计划
     * 使用自定义线程池 fitnessPlanExecutor
     */
    @Async("fitnessPlanExecutor")
    @Transactional
    public void asyncGeneratePlan(Long planId, UserPreference preference) {
        try {
            log.info("开始异步生成健身计划，planId: {}", planId);

            // a. 构建 prompt
            UserPreferenceRequest request = convertToRequest(preference);
            String prompt = FitnessPlanPromptBuilder.buildPrompt(request);

            // b. 调用 AI
            String aiResponse = fitnessPlanAiService.generateWeeklyPlan(prompt);

            // c. 解析 JSON
            WeeklyPlanResult result;
            try {
                result = objectMapper.readValue(aiResponse, WeeklyPlanResult.class);
            } catch (JsonProcessingException e) {
                log.error("AI 返回 JSON 解析失败，planId: {}, response: {}", planId, aiResponse, e);
                updatePlanStatus(planId, WeeklyPlan.STATUS_FAILED);
                return;
            }

            // d. 保存结果到数据库
            savePlanResult(planId, result);

            // e. 更新计划状态为 done
            updatePlanStatus(planId, WeeklyPlan.STATUS_DONE, result.title(), result.summary());

            log.info("健身计划生成完成，planId: {}", planId);
        } catch (Exception e) {
            log.error("健身计划生成异常，planId: {}", planId, e);
            updatePlanStatus(planId, WeeklyPlan.STATUS_FAILED);
        }
    }

    private UserPreferenceRequest convertToRequest(UserPreference preference) {
        List<String> focusMuscles;
        try {
            focusMuscles = objectMapper.readValue(preference.getFocusMuscles(), List.class);
        } catch (JsonProcessingException e) {
            focusMuscles = new ArrayList<>();
        }
        return new UserPreferenceRequest(
                preference.getExperienceLevel(),
                preference.getGoal(),
                focusMuscles,
                preference.getEquipment(),
                preference.getSessionDuration(),
                preference.getTrainingDaysPerWeek(),
                preference.getTrainingStyle(),
                preference.getInjuryNotes()
        );
    }

    private void savePlanResult(Long planId, WeeklyPlanResult result) {
        int sort;
        for (DayResult day : result.days()) {
            // 保存每日训练
            DailyTraining training = DailyTraining.builder()
                    .planId(planId)
                    .dayOfWeek(day.dayOfWeek())
                    .isRestDay(day.isRestDay() ? (byte) 1 : (byte) 0)
                    .focusMuscleGroup(day.focusMuscleGroup())
                    .warmUpNotes(day.warmUpNotes())
                    .coolDownNotes(day.coolDownNotes())
                    .build();
            dailyTrainingService.save(training);

            // 如果是休息日，不需要动作
            if (day.isRestDay() || day.exercises() == null) {
                continue;
            }

            // 保存每个动作
            sort = 0;
            for (ExerciseResult exercise : day.exercises()) {
                DailyExerciseItem item = DailyExerciseItem.builder()
                        .dailyTrainingId(training.getId())
                        .nameZh(exercise.nameZh())
                        .nameEn(exercise.nameEn())
                        .sortOrder(sort++)
                        .sets(exercise.sets())
                        .reps(exercise.reps())
                        .restSeconds(exercise.restSeconds())
                        .coachNotes(exercise.coachNotes())
                        .bilibiliUrl(VideoLinkBuilder.buildBilibiliUrl(exercise.nameZh()))
                        .build();
                dailyExerciseItemService.save(item);
            }
        }
    }

    private void updatePlanStatus(Long planId, String status) {
        updatePlanStatus(planId, status, null, null);
    }

    private void updatePlanStatus(Long planId, String status, String title, String summary) {
        // 使用 lambdaUpdate 只更新必要字段，符合规范五
        weeklyPlanService.lambdaUpdate()
                .eq(WeeklyPlan::getId, planId)
                .set(WeeklyPlan::getStatus, status)
                .set(title != null, WeeklyPlan::getTitle, title)
                .set(summary != null, WeeklyPlan::getSummary, summary)
                .update();
    }
}
