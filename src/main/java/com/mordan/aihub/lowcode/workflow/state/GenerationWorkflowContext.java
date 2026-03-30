package com.mordan.aihub.lowcode.workflow.state;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

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
     * 上一版本代码快照（迭代时使用）
     */
    private String parentCodeSnapshot;

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
     * 生成的代码结果，包含多个文件
     */
    private GeneratedCode generatedCode;

    /**
     * 校验错误列表
     */
    private List<String> validationErrors;

    /**
     * 当前重试次数
     */
    private Integer retryCount;

    /**
     * 最终通过校验的代码
     */
    private GeneratedCode finalCode;

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
     * LLM校验建议
     */
    private String llmSuggestions;

    /**
     * 持久化构建输出的路径（开启 persist-build-output 时才有值）
     */
    private String persistedBuildPath;
}
