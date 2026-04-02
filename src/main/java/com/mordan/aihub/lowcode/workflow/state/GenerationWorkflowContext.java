package com.mordan.aihub.lowcode.workflow.state;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 代码生成工作流上下文
 * 存储所有状态信息，便于统一维护
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationWorkflowContext implements Serializable {

    @Serial
    private static final long serialVersionUID = -1152363458317985722L;

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 用户输入提示词
     */
    private String userPrompt;

    /**
     * 用户提供的API文档文本
     */
    private String apiDocText;

    /**
     * 意图检查结果
     */
    private IntentCheckResult intentCheckResult;

    /**
     * 解析后的结构化意图
     */
    private ParsedIntent parsedIntent;

    /**
     * 原始解析得到的 JSON 字符串（保留用于日志和调试）
     */
    private String parsedIntentJson;

    /**
     * 生成的代码结果
     */
    private GeneratedResult generatedResult;

    /**
     * 当前重试次数
     */
    private Integer retryCount;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 是否成功
     */
    private Boolean codeSuccess;

    /**
     * 失败原因
     */
    private String failureReason;

    /**
     * 持久化构建输出的路径（开启 persist-build-output 时才有值）
     */
    private String persistedBuildPath;

    /**
     * 已有项目的文件摘要（迭代修改时使用，来自上一次生成）
     */
    private String existingProjectSummary;

    /**
     * 本次构建目录前缀（随机生成，用于隔离不同构建）
     */
    private String buildDirPrefix;

    /**
     * 质量检查结果
     */
    private QualityResult qualityResult;

}
