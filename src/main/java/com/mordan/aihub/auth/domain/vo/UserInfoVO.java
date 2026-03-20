package com.mordan.aihub.auth.domain.vo;

import java.time.LocalDateTime;

/**
 * 用户信息响应
 *
 * @author auth
 */
public record UserInfoVO(
    /**
     * 用户ID
     */
    Long userId,

    /**
     * 用户名
     */
    String username,

    /**
     * 昵称
     */
    String nickname,

    /**
     * 头像地址
     */
    String avatarUrl,

    /**
     * 创建时间
     */
    LocalDateTime createdAt
) {}
