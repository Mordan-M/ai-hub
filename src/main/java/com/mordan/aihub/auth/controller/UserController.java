package com.mordan.aihub.auth.controller;

import com.mordan.aihub.auth.domain.vo.LoginRequest;
import com.mordan.aihub.auth.domain.vo.LoginVO;
import com.mordan.aihub.auth.domain.vo.RegisterRequest;
import com.mordan.aihub.auth.domain.vo.UserInfoVO;
import com.mordan.aihub.auth.service.UserService;
import com.mordan.aihub.common.utils.ResultUtils;
import com.mordan.aihub.common.vo.BaseResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 用户登录注册控制器
 *
 * @author auth
 */
@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 用户注册
     * 无需登录
     */
    @PostMapping("/register")
    public BaseResponse<UserInfoVO> register(@Valid @RequestBody RegisterRequest request) {
        UserInfoVO userInfo = userService.register(request);
        return ResultUtils.success(userInfo);
    }

    /**
     * 用户登录
     * 无需登录
     */
    @PostMapping("/login")
    public BaseResponse<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        LoginVO loginResult = userService.login(request);
        return ResultUtils.success(loginResult);
    }

    /**
     * 用户登出
     * 需要登录
     */
    @PostMapping("/logout")
    public BaseResponse<Void> logout() {
        userService.logout();
        return ResultUtils.success(null);
    }

    /**
     * 获取当前登录用户信息
     * 需要登录，userId 从 SecurityContextHolder 中取，不从路径参数传
     */
    @GetMapping("/me")
    public BaseResponse<UserInfoVO> getCurrentUserInfo() {
        Long userId = userService.getCurrentUserId();
        UserInfoVO userInfo = userService.getUserInfo(userId);
        return ResultUtils.success(userInfo);
    }
}
