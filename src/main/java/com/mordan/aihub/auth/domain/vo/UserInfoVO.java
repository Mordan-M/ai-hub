package com.mordan.aihub.auth.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息响应
 *
 * @author auth
 */
@Data
public class UserInfoVO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像地址
     */
    private String avatarUrl;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
