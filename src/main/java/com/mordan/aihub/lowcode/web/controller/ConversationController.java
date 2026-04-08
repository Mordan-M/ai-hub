package com.mordan.aihub.lowcode.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mordan.aihub.auth.service.UserService;
import com.mordan.aihub.common.utils.ResultUtils;
import com.mordan.aihub.common.vo.BaseResponse;
import com.mordan.aihub.lowcode.domain.service.ConversationService;
import com.mordan.aihub.lowcode.web.vo.MessageVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对话消息Controller
 */
@RestController
@RequestMapping("/ai-hub/lowcode/apps/{appId}/conversations")
public class ConversationController {

    @Resource
    private UserService userService;

    @Resource
    private ConversationService conversationService;

    /**
     * 获取对话历史（分页）
     */
    @GetMapping
    public BaseResponse<IPage<MessageVO>> listMessages(
            @PathVariable Long appId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        Long userId = userService.getCurrentUserId();
        IPage<MessageVO> result = conversationService.listMessages(userId, appId, page, size);
        return ResultUtils.success(result);
    }

    /**
     * 清空对话历史
     */
    @DeleteMapping
    public BaseResponse<Void> clearHistory(@PathVariable Long appId) {
        Long userId = userService.getCurrentUserId();
        conversationService.clearHistory(userId, appId);
        return ResultUtils.success(null);
    }
}
