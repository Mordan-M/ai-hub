package com.mordan.aihub.auth.domain.vo;

import lombok.Data;

/**
 * 登录响应结果
 *
 * @author auth
 */
@Data
public class LoginVO {

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
     * JWT Token，前端存入 localStorage 后续请求放在 Header Authorization 中
     */
    private String token;
}
