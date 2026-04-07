package com.mordan.aihub.lowcode.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mordan.aihub.auth.service.UserService;
import com.mordan.aihub.common.utils.ResultUtils;
import com.mordan.aihub.common.vo.BaseResponse;
import com.mordan.aihub.lowcode.domain.entity.GenerationTask;
import com.mordan.aihub.lowcode.domain.enums.TaskStatus;
import com.mordan.aihub.lowcode.domain.service.GenerationTaskService;
import com.mordan.aihub.lowcode.infrastructure.sse.SseEmitterRegistry;
import com.mordan.aihub.lowcode.mapper.GenerationTaskMapper;
import com.mordan.aihub.lowcode.web.request.GenerateRequest;
import com.mordan.aihub.lowcode.web.vo.TaskVO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

/**
 * 代码生成Controller
 * 包含SSE流式进度推送端点
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/lowcode/apps/{appId}")
public class GenerationController {

    @Resource
    private UserService userService;

    @Resource
    private GenerationTaskMapper generationTaskMapper;

    @Resource
    private GenerationTaskService generationTaskService;

    @Resource
    private SseEmitterRegistry sseEmitterRegistry;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 提交代码生成任务
     */
    @PostMapping("/generate")
    public BaseResponse<TaskVO> submitGenerateTask(
            @PathVariable Long appId,
            @Valid @RequestBody GenerateRequest request) {
        Long userId = userService.getCurrentUserId();
        TaskVO taskVO = generationTaskService.submitGenerateTask(userId, appId, request);
        return ResultUtils.success(taskVO);
    }

    /**
     * SSE流式连接，推送生成进度
     */
    @GetMapping("/tasks/{taskId}/stream")
    public SseEmitter streamProgress(
            @PathVariable Long taskId) {
        Long userId = userService.getCurrentUserId();
        // 鉴权
        generationTaskService.getTaskStatus(userId, taskId);

        SseEmitter emitter = new SseEmitter(SseEmitterRegistry.TIMEOUT_MS);
        sseEmitterRegistry.register(taskId.toString(), emitter);

        // 立即推送连接成功事件
        try {
            Map<String, String> connectedEvent = Map.of(
                    "stage", "connected",
                    "message", "已连接，等待生成进度..."
            );
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(objectMapper.writeValueAsString(connectedEvent)));

            // 如果任务已经完成，直接推送终态
            GenerationTask task = generationTaskMapper.selectById(taskId);
            if (task != null && task.getStatus() == TaskStatus.SUCCESS) {
                // 这里不需要处理，已经完成，前端可以直接轮询
                sseEmitterRegistry.complete(taskId.toString());
            } else if (task != null && task.getStatus() == TaskStatus.FAILED) {
                sseEmitterRegistry.sendError(taskId.toString(), task.getErrorMessage());
                sseEmitterRegistry.complete(taskId.toString());
            }

        } catch (IOException e) {
            log.warn("Failed to send connected event", e);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * 获取任务状态
     */
    @GetMapping("/tasks/{taskId}")
    public BaseResponse<TaskVO> getTaskStatus(
            @PathVariable Long taskId) {
        Long userId = userService.getCurrentUserId();
        TaskVO taskVO = generationTaskService.getTaskStatus(userId, taskId);
        return ResultUtils.success(taskVO);
    }

}
