package com.mordan.aihub.auth.domain.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户注册请求入参
 *
 * @author auth
 */
public record RegisterRequest(
    /**
     * 用户名，3-20 个字符
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在 3-20 之间")
    String username,

    /**
     * 密码，6-30 个字符
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 30, message = "密码长度必须在 6-30 之间")
    String password,

    /**
     * 昵称（可选）
     */
    String nickname
) {}
