package com.mordan.aihub.lowcode.domain.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mordan.aihub.lowcode.config.GenerationProperties;
import com.mordan.aihub.lowcode.domain.entity.GeneratedVersion;
import com.mordan.aihub.lowcode.domain.entity.GenerationTask;
import com.mordan.aihub.lowcode.domain.enums.TaskStatus;
import com.mordan.aihub.lowcode.mapper.GenerationTaskMapper;
import com.mordan.aihub.lowcode.domain.service.ApplicationService;
import com.mordan.aihub.lowcode.domain.service.ConversationService;
import com.mordan.aihub.lowcode.domain.service.GenerationTaskService;
import com.mordan.aihub.lowcode.mapper.GeneratedVersionMapper;
import com.mordan.aihub.lowcode.workflow.CodeGenerationWorkflow;
import com.mordan.aihub.lowcode.workflow.state.GenerationWorkflowContext;
import com.mordan.aihub.lowcode.workflow.state.WorkflowState;
import com.mordan.aihub.lowcode.web.request.GenerateRequest;
import com.mordan.aihub.lowcode.web.vo.TaskVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 生成任务服务实现
 */
@Slf4j
@Service
public class GenerationTaskServiceImpl extends ServiceImpl<GenerationTaskMapper, GenerationTask>
        implements GenerationTaskService {

    @Resource
    private ApplicationService applicationService;
    @Resource
    private ConversationService conversationService;
    @Resource
    private GeneratedVersionMapper generatedVersionMapper;
    @Resource
    private GenerationProperties generationProperties;
    @Resource
    private CodeGenerationWorkflow codeGenerationWorkflow;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public TaskVO submitGenerateTask(Long userId, Long appId, GenerateRequest req) {
        // 1. 鉴权：验证应用归属
        applicationService.getAppDetail(userId, appId);

        // 2. 检查并发任务数
        long runningCount = this.lambdaQuery()
                .eq(GenerationTask::getUserId, userId)
                .eq(GenerationTask::getStatus, TaskStatus.RUNNING)
                .count();
        if (runningCount >= generationProperties.getMaxConcurrentTasksPerUser()) {
            throw new RuntimeException(String.format("您已有%d个正在运行的生成任务，请等待完成后再提交", runningCount));
        }

        // 3. 保存用户消息到对话
        conversationService.saveUserMessage(userId, appId, req.getPrompt());

        // 4. 创建任务记录
        GenerationTask task = GenerationTask.builder()
                .appId(appId)
                .userId(userId)
                .prompt(req.getPrompt())
                .apiDocText(req.getApiDocText())
                .parentVersionId(req.getBaseVersionId())
                .status(TaskStatus.PENDING)
                .retryCount(0)
                .maxRetry(generationProperties.getMaxRetry())
                .build();
        save(task);
        task.setWorkflowThreadId("task_" + task.getId());
        updateById(task);

        // 5. 准备初始状态
        String parentCodeSnapshot = null;
        if (req.getBaseVersionId() != null) {
            GeneratedVersion baseVersion = generatedVersionMapper.selectById(req.getBaseVersionId());
            if (baseVersion != null && baseVersion.getCodeStoragePath() != null) {
                // 读取已有代码文件，构建parentCodeSnapshot（JSON格式）
                parentCodeSnapshot = readCodeSnapshot(baseVersion.getCodeStoragePath());
            }
        }

        // 使用 GenerationWorkflowContext 封装所有初始状态
        GenerationWorkflowContext context = GenerationWorkflowContext.builder()
                .appId(appId.toString())
                .userId(userId.toString())
                .taskId(task.getId().toString())
                .userPrompt(req.getPrompt())
                .apiDocText(req.getApiDocText() != null ? req.getApiDocText() : "")
                .parentCodeSnapshot(parentCodeSnapshot != null ? parentCodeSnapshot : "")
                .build();

        Map<String, Object> initialState = Map.of(WorkflowState.CONTEXT_KEY, context);

        // 6. 更新任务状态为RUNNING
        task.setStatus(TaskStatus.RUNNING);
        updateById(task);

        // 7. 异步提交工作流
        codeGenerationWorkflow.submit(initialState, task.getId());

        log.info("Generation task submitted: taskId={}, userId={}, appId={}", task.getId(), userId, appId);
        return toVO(task);
    }

    @Override
    public TaskVO getTaskStatus(Long userId, Long taskId) {
        GenerationTask task = this.lambdaQuery()
                .eq(GenerationTask::getId, taskId)
                .eq(GenerationTask::getUserId, userId)
                .one();
        if (task == null) {
            throw new RuntimeException("任务不存在或无权访问");
        }
        return toVO(task);
    }

    @Override
    public List<TaskVO> listTasksByApp(Long userId, Long appId) {
        // 鉴权
        applicationService.getAppDetail(userId, appId);

        List<GenerationTask> tasks = this.lambdaQuery()
                .eq(GenerationTask::getAppId, appId)
                .eq(GenerationTask::getUserId, userId)
                .orderByDesc(GenerationTask::getCreatedAt)
                .list();
        return tasks.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 读取已有版本代码作为快照
     */
    private String readCodeSnapshot(String storagePath) {
        try {
            var root = Path.of(storagePath);
            var files = Files.walk(root)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().matches(".*\\.(jsx|js|html|css|json)$"))
                    .map(p -> {
                        try {
                            return Map.of(
                                    "path", root.relativize(p).toString().replace('\\', '/'),
                                    "content", Files.readString(p)
                            );
                        } catch (IOException e) {
                            return null;
                        }
                    })
                    .filter(m -> m != null)
                    .toList();
            return objectMapper.writeValueAsString(Map.of("files", files));
        } catch (Exception e) {
            log.warn("Failed to read parent code snapshot", e);
            return null;
        }
    }

    /**
     * 转换为VO
     */
    private TaskVO toVO(GenerationTask task) {
        return TaskVO.builder()
                .id(task.getId())
                .appId(task.getAppId())
                .status(task.getStatus())
                .retryCount(task.getRetryCount())
                .errorMessage(task.getErrorMessage())
                .createdAt(task.getCreatedAt())
                .finishedAt(task.getFinishedAt())
                .build();
    }
}
