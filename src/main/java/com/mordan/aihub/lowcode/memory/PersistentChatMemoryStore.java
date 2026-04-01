package com.mordan.aihub.lowcode.memory;

import com.mordan.aihub.lowcode.domain.entity.ChatMemory;
import com.mordan.aihub.lowcode.domain.enums.MessageRole;
import com.mordan.aihub.lowcode.domain.service.ChatMemoryService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.baomidou.mybatisplus.core.toolkit.Wrappers.lambdaQuery;

/**
 * 基于数据库的持久化对话记忆存储
 * 负责将 LangChain4j ChatMessage 持久化到 chat_memory 表
 * 与业务对话消息（conversation_message）分离，避免写入策略冲突
 */
@Slf4j
@Component
public class PersistentChatMemoryStore implements ChatMemoryStore {

    @Resource
    private ChatMemoryService chatMemoryService;

    // ----------------------------------------------------------------
    // ChatMemoryStore 接口实现
    // ----------------------------------------------------------------

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String id = memoryId.toString();
        log.debug("Loading chat memory from DB, memoryId={}", id);

        List<ChatMemory> entities = chatMemoryService.list(
                lambdaQuery(ChatMemory.class)
                        .eq(ChatMemory::getMemoryId, id)
                        .orderByAsc(ChatMemory::getSeq)
        );

        List<ChatMessage> messages = new ArrayList<>(entities.size());
        for (ChatMemory entity : entities) {
            try {
                ChatMessage message = deserialize(entity);
                if (message != null) {
                    messages.add(message);
                }
            } catch (Exception e) {
                // 单条反序列化失败不中断整体，跳过该条记录
                log.warn("Failed to deserialize message, id={}, role={}, skipping",
                        entity.getId(), entity.getRole(), e);
            }
        }

        log.debug("Loaded {} messages from DB, memoryId={}", messages.size(), id);
        return messages;
    }

    @Override
    @Transactional
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String id = memoryId.toString();

        // 过滤 SystemMessage：由 @SystemMessage 注解每次自动注入，无需持久化
        List<ChatMessage> toSave = messages.stream()
                .toList();

        log.debug("Updating chat memory, memoryId={}, total={}, toSave={}",
                id, messages.size(), toSave.size());

        // 全量覆盖：LangChain4j 每次传入完整列表
        chatMemoryService.remove(
                lambdaQuery(ChatMemory.class)
                        .eq(ChatMemory::getMemoryId, id)
        );

        List<ChatMemory> entities = new ArrayList<>(toSave.size());
        for (int i = 0; i < toSave.size(); i++) {
            ChatMessage msg = toSave.get(i);
            entities.add(ChatMemory.builder()
                    .memoryId(id)
                    .seq(i + 1)
                    .role(resolveRole(msg))
                    .content(serialize(msg))
                    .contentText(extractText(msg))
                    .createdAt(System.currentTimeMillis())
                    .build());
        }

        chatMemoryService.saveBatch(entities);
        log.debug("Saved {} messages to DB, memoryId={}", entities.size(), id);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        String id = memoryId.toString();
        log.info("Deleting all chat memory, memoryId={}", id);
        chatMemoryService.remove(
                lambdaQuery(ChatMemory.class)
                        .eq(ChatMemory::getMemoryId, id)
        );
    }

    // ----------------------------------------------------------------
    // 序列化 / 反序列化
    // ----------------------------------------------------------------

    /**
     * 将 ChatMessage 序列化为 JSON 字符串（使用 LangChain4j 官方工具）
     */
    private String serialize(ChatMessage message) {
        return ChatMessageSerializer.messageToJson(message);
    }

    /**
     * 将数据库记录反序列化为 ChatMessage
     */
    private ChatMessage deserialize(ChatMemory entity) {
        return ChatMessageDeserializer.messageFromJson(entity.getContent());
    }

    // ----------------------------------------------------------------
    // 工具方法
    // ----------------------------------------------------------------

    /**
     * 解析消息角色，用于写入 role 字段
     */
    private MessageRole resolveRole(ChatMessage message) {
        return switch (message) {
            case UserMessage ignored             -> MessageRole.USER;
            case AiMessage ignored               -> MessageRole.ASSISTANT;
            case ToolExecutionResultMessage ignored -> MessageRole.TOOL;
            case SystemMessage ignored -> MessageRole.SYSTEM;
            default -> {
                log.warn("Unknown message type: {}", message.getClass().getSimpleName());
                yield MessageRole.USER;
            }
        };
    }

    /**
     * 提取消息纯文本，用于前端展示；非文本消息（如 tool result）返回 null
     */
    private String extractText(ChatMessage message) {
        return switch (message) {
            case UserMessage msg -> msg.singleText();
            case AiMessage msg  -> msg.text();
            case ToolExecutionResultMessage msg -> msg.text(); // tool 执行结果文本
            default -> null;
        };
    }
}
