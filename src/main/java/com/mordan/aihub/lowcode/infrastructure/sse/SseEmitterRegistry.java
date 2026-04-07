package com.mordan.aihub.lowcode.infrastructure.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE Emitter注册中心
 * 维护 taskId -> SseEmitter 的映射，用于工作流向前端推送生成进度
 */
@Slf4j
@Component
public class SseEmitterRegistry {

    public static final long TIMEOUT_MS = 600000L; // 10分钟超时

    private final ConcurrentHashMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public SseEmitterRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 注册一个新的SSE Emitter
     * @param taskId 任务ID
     * @param emitter SseEmitter实例
     */
    public void register(String taskId, SseEmitter emitter) {
        emitter.onCompletion(() -> {
            log.debug("SSE emitter completed for task: {}", taskId);
            remove(taskId);
        });
        emitter.onTimeout(() -> {
            log.warn("SSE emitter timeout for task: {}", taskId);
            remove(taskId);
        });
        emitter.onError((e) -> {
            log.error("SSE emitter error for task: {}", taskId, e);
            remove(taskId);
        });
        emitters.put(taskId, emitter);
        log.debug("Registered SSE emitter for task: {}", taskId);
    }

    /**
     * 推送进度事件
     * @param taskId 任务ID
     * @param stage 当前阶段标识
     * @param message 当前阶段描述
     * @param retryCount 当前重试次数
     */
    public void sendProgress(String taskId, String stage, String message, int retryCount) {
        ProgressEvent event = new ProgressEvent(stage, message, retryCount);
        sendEvent(taskId, "progress", event);
    }

    /**
     * 推送完成事件
     * @param taskId 任务ID
     * @param versionId 生成成功的版本ID
     * @param previewUrl 预览地址
     */
    public void sendComplete(String taskId, Long versionId, String previewUrl) {
        CompleteEvent event = new CompleteEvent(versionId, previewUrl);
        sendEvent(taskId, "complete", event);
    }

    /**
     * 推送拒绝事件
     * @param taskId 任务ID
     * @param message 提示消息
     * @param reason 拒绝原因
     */
    public void sendRejected(String taskId, String message, String reason) {
        RejectedEvent event = new RejectedEvent(message, reason);
        sendEvent(taskId, "rejected", event);
    }

    /**
     * 推送错误事件
     * @param taskId 任务ID
     * @param message 错误消息
     */
    public void sendError(String taskId, String message) {
        ErrorEvent event = new ErrorEvent(message);
        sendEvent(taskId, "error", event);
    }

    /**
     * 完成并移除Emitter
     * @param taskId 任务ID
     */
    public void complete(String taskId) {
        SseEmitter emitter = emitters.get(taskId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.warn("Error completing SSE emitter for task {}", taskId, e);
            }
        }
        remove(taskId);
    }

    /**
     * 直接移除Emitter
     * @param taskId 任务ID
     */
    public void remove(String taskId) {
        emitters.remove(taskId);
        log.debug("Removed SSE emitter for task: {}", taskId);
    }

    /**
     * 获取Emitter（供测试用）
     */
    public SseEmitter get(String taskId) {
        return emitters.get(taskId);
    }

    /**
     * 发送通用事件
     */
    private void sendEvent(String taskId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(taskId);
        if (emitter == null) {
            log.debug("No SSE emitter registered for task: {}", taskId);
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(data);
            emitter.send(SseEmitter.event().name(eventName).data(json));
        } catch (IOException e) {
            log.warn("Failed to send SSE event {} for task {}", eventName, taskId, e);
            remove(taskId);
        }
    }

    // Event DTOs
    @Data
    public static class ProgressEvent {
        private final String stage;
        private final String message;
        private final int retryCount;
        public ProgressEvent(String stage, String message, int retryCount) {
            this.stage = stage;
            this.message = message;
            this.retryCount = retryCount;
        }
    }

    @Data
    public static class CompleteEvent {
        private final Long versionId;
        private final String previewUrl;
        public CompleteEvent(Long versionId, String previewUrl) {
            this.versionId = versionId;
            this.previewUrl = previewUrl;
        }
    }

    @Data
    public static class RejectedEvent {
        private final String message;
        private final String reason;
        public RejectedEvent(String message, String reason) {
            this.message = message;
            this.reason = reason;
        }
    }

    @Data
    public static class ErrorEvent {
        private final String message;
        public ErrorEvent(String message) {
            this.message = message;
        }
    }
}
