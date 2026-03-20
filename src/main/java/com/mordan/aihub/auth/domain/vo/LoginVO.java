package com.mordan.aihub.auth.domain.vo;

/**
 * 登录响应结果
 *
 * @author auth
 */
public record LoginVO(
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
     * JWT Token，前端存入 localStorage 后续请求放在 Header Authorization 中
     */
    String token
) {}
