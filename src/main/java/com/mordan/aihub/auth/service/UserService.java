package com.mordan.aihub.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mordan.aihub.auth.domain.vo.LoginRequest;
import com.mordan.aihub.auth.domain.vo.LoginVO;
import com.mordan.aihub.auth.domain.vo.RegisterRequest;
import com.mordan.aihub.auth.domain.vo.UserInfoVO;
import com.mordan.aihub.fitness.domain.entity.User;

/**
 * 用户业务服务接口
 * 提供注册、登录、登出、查询用户信息功能
 *
 * @author auth
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 注册成功的用户信息
     */
    UserInfoVO register(RegisterRequest request);

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录结果（含 token）
     */
    LoginVO login(LoginRequest request);

    /**
     * 用户登出
     * 当前为无状态 JWT 方案，服务端无需操作
     */
    void logout();

    /**
     * 根据 userId 查询用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    UserInfoVO getUserInfo(Long userId);

    /**
     * 从 SecurityContextHolder 中获取当前登录用户的 userId
     * 供其他模块（如健身助手模块）调用，避免重复解析 Token
     *
     * @return 当前登录用户ID，如果未登录返回 null
     */
    Long getCurrentUserId();
}
