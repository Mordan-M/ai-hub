package com.mordan.aihub.fitness.service;

import com.mordan.aihub.fitness.domain.vo.UserPreferenceRequest;

import java.util.List;

/**
 * 健身计划 Prompt 构建器
 *
 * @author fitness
 */
public class FitnessPlanPromptBuilder {

    /**
     * 根据用户偏好构建生成健身计划的用户 Prompt
     * 系统提示和格式要求已放在系统提示词文件中
     */
    public static String buildPrompt(UserPreferenceRequest request) {
        StringBuilder sb = new StringBuilder();

        sb.append("用户健身信息：\n");

        // 经验等级
        String experienceLevelText = translateExperienceLevel(request.getExperienceLevel());
        sb.append("- 经验等级：").append(experienceLevelText).append("\n");

        // 训练目标
        String goalText = translateGoal(request.getGoal());
        sb.append("- 训练目标：").append(goalText).append("\n");

        // 重点锻炼部位
        if (request.getFocusMuscles() != null && !request.getFocusMuscles().isEmpty()) {
            String focusMusclesText = translateFocusMuscles(request.getFocusMuscles());
            sb.append("- 重点锻炼部位：").append(focusMusclesText).append("\n");
        }

        // 可用器材
        String equipmentText = translateEquipment(request.getEquipment());
        sb.append("- 可用器材：").append(equipmentText).append("\n");

        // 每次训练时长
        String durationText = translateSessionDuration(request.getSessionDuration());
        sb.append("- 每次训练可花费时长：").append(durationText).append("\n");

        // 每周训练天数
        sb.append("- 每周可训练天数：").append(request.getTrainingDaysPerWeek()).append("天\n");

        // 训练风格
        String styleText = translateTrainingStyle(request.getTrainingStyle());
        sb.append("- 偏好训练风格：").append(styleText).append("\n");

        // 受伤注意事项
        if (request.getInjuryNotes() != null && !request.getInjuryNotes().isBlank()) {
            sb.append("- 特殊注意事项：").append(request.getInjuryNotes()).append("\n");
        }

        return sb.toString();
    }

    private static String translateExperienceLevel(String level) {
        return switch (level) {
            case "beginner" -> "新手";
            case "intermediate" -> "进阶";
            case "advanced" -> "老手";
            default -> level;
        };
    }

    private static String translateGoal(String goal) {
        return switch (goal) {
            case "muscle_gain" -> "增肌";
            case "fat_loss" -> "减脂";
            case "body_shaping" -> "塑形";
            case "endurance" -> "提升耐力";
            case "general_health" -> "保持健康";
            default -> goal;
        };
    }

    private static String translateFocusMuscles(List<String> muscles) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < muscles.size(); i++) {
            sb.append(translateMuscle(muscles.get(i)));
            if (i < muscles.size() - 1) {
                sb.append("、");
            }
        }
        return sb.toString();
    }

    private static String translateMuscle(String muscle) {
        return switch (muscle) {
            case "chest" -> "胸肌";
            case "back" -> "背部";
            case "arms" -> "手臂";
            case "shoulders" -> "肩膀";
            case "legs" -> "腿部";
            case "core" -> "核心";
            case "glutes" -> "臀部";
            case "abs" -> "腹肌";
            default -> muscle;
        };
    }

    private static String translateEquipment(String equipment) {
        return switch (equipment) {
            case "none" -> "无器械（仅自重）";
            case "dumbbell" -> "哑铃";
            case "barbell" -> "杠铃";
            case "gym_machine" -> "健身房全套器械";
            case "home_equipment" -> "家用健身器材";
            default -> equipment;
        };
    }

    private static String translateSessionDuration(String duration) {
        return switch (duration) {
            case "under_30" -> "30分钟以内";
            case "30_to_60" -> "30-60分钟";
            case "60_to_90" -> "60-90分钟";
            case "above_90" -> "90分钟以上";
            default -> duration;
        };
    }

    private static String translateTrainingStyle(String style) {
        return switch (style) {
            case "split" -> "分化训练";
            case "full_body" -> "全身训练";
            case "hiit" -> "高强度间歇训练";
            case "circuit" -> "循环训练";
            case "strength" -> "力量训练";
            default -> style;
        };
    }
}
