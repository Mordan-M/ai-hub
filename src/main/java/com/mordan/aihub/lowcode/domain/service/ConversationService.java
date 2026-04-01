package com.mordan.aihub.lowcode.domain.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mordan.aihub.lowcode.domain.entity.ConversationMessage;
import com.mordan.aihub.lowcode.web.vo.MessageVO;

/**
 * 对话消息服务接口
 */
public interface ConversationService extends IService<ConversationMessage> {

    /**
     * 保存用户消息
     * @param userId 用户ID
     * @param appId 应用ID
     * @param content 消息内容
     * @return 消息VO
     */
    MessageVO saveUserMessage(Long userId, Long appId, String content);

    /**
     * 保存助手消息
     * @param userId 用户ID
     * @param appId 应用ID
     * @param content 消息内容
     * @param taskId 关联任务ID
     * @return 消息VO
     */
    MessageVO saveAssistantMessage(Long userId, Long appId, String content, Long taskId);

    /**
     * 查询应用对话历史
     * @param userId 用户ID（鉴权用）
     * @param appId 应用ID
     * @param page 页码
     * @param size 每页大小
     * @return 分页消息VO列表
     */
    IPage<MessageVO> listMessages(Long userId, Long appId, int page, int size);

    /**
     * 清空应用对话历史
     * @param userId 用户ID（鉴权用）
     * @param appId 应用ID
     */
    void clearHistory(Long userId, Long appId);
}
