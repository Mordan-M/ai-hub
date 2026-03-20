package com.mordan.aihub.auth.domain.vo;

import jakarta.validation.constraints.NotBlank;

/**
 * 用户登录请求入参
 *
 * @author auth
 */
public record LoginRequest(
    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    String username,

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    String password
) {}
