package com.mordan.aihub.lowcode.domain.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mordan.aihub.lowcode.domain.entity.ConversationMessage;
import com.mordan.aihub.lowcode.domain.entity.GeneratedVersion;
import com.mordan.aihub.lowcode.domain.enums.MessageRole;
import com.mordan.aihub.lowcode.mapper.ConversationMessageMapper;
import com.mordan.aihub.lowcode.mapper.GeneratedVersionMapper;
import com.mordan.aihub.lowcode.domain.service.ApplicationService;
import com.mordan.aihub.lowcode.domain.service.ConversationService;
import com.mordan.aihub.lowcode.web.vo.MessageVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 对话消息服务实现
 */
@Service
public class ConversationServiceImpl extends ServiceImpl<ConversationMessageMapper, ConversationMessage>
        implements ConversationService {

    @Resource
    private GeneratedVersionMapper generatedVersionMapper;
    @Resource
    private ApplicationService applicationService;

    @Override
    public MessageVO saveUserMessage(Long userId, Long appId, String content) {
        // 鉴权：验证应用归属
        applicationService.getAppDetail(userId, appId);

        ConversationMessage message = ConversationMessage.builder()
                .appId(appId)
                .userId(userId)
                .role(MessageRole.USER)
                .content(content)
                .build();
        save(message);
        return toVO(message, null);
    }

    @Override
    public MessageVO saveAssistantMessage(Long userId, Long appId, String content,
                                          Long taskId, Long versionId) {
        // 鉴权：验证应用归属
        applicationService.getAppDetail(userId, appId);

        ConversationMessage message = ConversationMessage.builder()
                .appId(appId)
                .userId(userId)
                .role(MessageRole.ASSISTANT)
                .content(content)
                .taskId(taskId)
                .versionId(versionId)
                .build();
        save(message);

        String previewUrl = null;
        if (versionId != null) {
            GeneratedVersion version = generatedVersionMapper.selectById(versionId);
            if (version != null) {
                previewUrl = version.getPreviewUrl();
            }
        }
        return toVO(message, previewUrl);
    }

    @Override
    public com.baomidou.mybatisplus.core.metadata.IPage<MessageVO> listMessages(Long userId, Long appId, int page, int size) {
        // 鉴权：验证应用归属
        applicationService.getAppDetail(userId, appId);

        Page<ConversationMessage> pageRequest = new Page<>(page, size);
        Page<ConversationMessage> resultPage = this.lambdaQuery()
                .eq(ConversationMessage::getAppId, appId)
                .eq(ConversationMessage::getUserId, userId)
                .orderByAsc(ConversationMessage::getCreatedAt)
                .page(pageRequest);

        return resultPage.convert(message -> {
            String previewUrl = null;
            if (message.getVersionId() != null) {
                GeneratedVersion version = generatedVersionMapper.selectById(message.getVersionId());
                if (version != null) {
                    previewUrl = version.getPreviewUrl();
                }
            }
            return toVO(message, previewUrl);
        });
    }

    @Override
    public void clearHistory(Long userId, Long appId) {
        // 鉴权：验证应用归属
        applicationService.getAppDetail(userId, appId);

        this.lambdaUpdate()
                .eq(ConversationMessage::getAppId, appId)
                .eq(ConversationMessage::getUserId, userId)
                .remove();
    }

    /**
     * 转换为VO
     */
    private MessageVO toVO(ConversationMessage message, String previewUrl) {
        return MessageVO.builder()
                .id(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .taskId(message.getTaskId())
                .versionId(message.getVersionId())
                .previewUrl(previewUrl)
                .createdAt(message.getCreatedAt())
                .build();
    }
}
