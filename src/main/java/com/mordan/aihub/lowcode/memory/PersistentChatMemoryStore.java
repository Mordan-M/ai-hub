package com.mordan.aihub.lowcode.ai.memory;

import com.mordan.aihub.lowcode.domain.entity.ConversationMessage;
import com.mordan.aihub.lowcode.entity.ConversationMessageEntity;
import com.mordan.aihub.lowcode.mapper.ConversationMessageMapper;
import com.mordan.aihub.lowcode.repository.ConversationMessageRepository;
import dev.langchain4j.data.message.*;
        import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于数据库的持久化对话记忆存储
 * 负责将 LangChain4j ChatMessage 持久化到 conversation_message 表
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PersistentChatMemoryStore implements ChatMemoryStore {

    @Resource
    private ConversationMessageMapper conversationMessageMapper;
    // ----------------------------------------------------------------
    // ChatMemoryStore 接口实现
    // ----------------------------------------------------------------

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String appId = memoryId.toString();
        log.debug("Loading chat memory from DB, appId={}", appId);

        List<ConversationMessage> entities =
                conversationMessageMapper.findByAppIdOrderBySeqAsc(appId);

        List<ChatMessage> messages = new ArrayList<>(entities.size());
        for (ConversationMessage entity : entities) {
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

        log.debug("Loaded {} messages from DB, appId={}", messages.size(), appId);
        return messages;
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String appId = memoryId.toString();

        // 过滤 SystemMessage：由 @SystemMessage 注解每次自动注入，无需持久化
        List<ChatMessage> toSave = messages.stream()
                .filter(msg -> !(msg instanceof SystemMessage))
                .toList();

        log.debug("Updating chat memory, appId={}, total={}, toSave={}",
                appId, messages.size(), toSave.size());

        // 全量覆盖：LangChain4j 每次传入完整列表
        conversationMessageMapper.deleteByAppId(appId);

        List<ConversationMessage> entities = new ArrayList<>(toSave.size());
        for (int i = 0; i < toSave.size(); i++) {
            ChatMessage msg = toSave.get(i);
            entities.add(ConversationMessage.builder()
                    .appId(Long.valueOf(appId))
                    .seq(i + 1)
                    .role(resolveRole(msg))
                    .content(serialize(msg))
                    .contentText(extractText(msg))
                    .createdAt(System.currentTimeMillis())
                    .build());
        }

        conversationMessageMapper.saveAll(entities);
        log.debug("Saved {} messages to DB, appId={}", entities.size(), appId);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        String appId = memoryId.toString();
        log.info("Deleting all chat memory, appId={}", appId);
        conversationMessageMapper.deleteByAppId(appId);
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
    private ChatMessage deserialize(ConversationMessage entity) {
        return ChatMessageDeserializer.messageFromJson(entity.getContent());
    }

    // ----------------------------------------------------------------
    // 工具方法
    // ----------------------------------------------------------------

    /**
     * 解析消息角色字符串，用于写入 role 字段
     */
    private String resolveRole(ChatMessage message) {
        return switch (message) {
            case UserMessage ignored             -> "USER";
            case AiMessage ignored               -> "ASSISTANT";
            case ToolExecutionResultMessage ignored -> "TOOL";
            default -> {
                log.warn("Unknown message type: {}", message.getClass().getSimpleName());
                yield "UNKNOWN";
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
