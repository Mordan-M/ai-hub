package com.mordan.aihub.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mordan.aihub.auth.domain.vo.LoginRequest;
import com.mordan.aihub.auth.domain.vo.LoginVO;
import com.mordan.aihub.auth.domain.vo.RegisterRequest;
import com.mordan.aihub.auth.domain.vo.UserInfoVO;
import com.mordan.aihub.auth.util.JwtTokenProvider;
import com.mordan.aihub.fitness.domain.entity.User;
import com.mordan.aihub.fitness.mapper.UserMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 用户业务服务
 * 提供注册、登录、登出、查询用户信息功能
 *
 * @author auth
 */
@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public UserService(UserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 注册成功的用户信息
     */
    public UserInfoVO register(RegisterRequest request) {
        // 1. 检查 username 是否已存在，存在则抛出业务异常（用户名已被占用）
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, request.getUsername());
        User existUser = userMapper.selectOne(queryWrapper);
        if (existUser != null) {
            throw new RuntimeException("用户名已被占用");
        }

        // 2. 密码用 BCryptPasswordEncoder 加密后存入 users 表
        // 3. nickname 若未填写，默认等于 username
        String nickname = request.getNickname();
        if (nickname == null || nickname.isBlank()) {
            nickname = request.getUsername();
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(nickname)
                .avatarUrl("https://img2.baidu.com/it/u=735961080,1059579988&fm=253&fmt=auto&app=120&f=JPEG?w=500&h=662")
                .status((byte) 1)
                .build();

        userMapper.insert(user);

        // 4. 返回注册成功的 UserInfoVO
        return convertToUserInfoVO(user);
    }

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return 登录结果（含 token）
     */
    public LoginVO login(LoginRequest request) {
        try {
            // 1. 调用 AuthenticationManager.authenticate() 触发 Spring Security 认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            // 2. 认证成功
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 根据用户名查询用户信息
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getUsername, request.getUsername());
            User user = userMapper.selectOne(queryWrapper);

            // 3. 生成 Token
            String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());

            // 4. 返回 LoginVO
            LoginVO loginVO = new LoginVO();
            loginVO.setUserId(user.getId());
            loginVO.setUsername(user.getUsername());
            loginVO.setNickname(user.getNickname());
            loginVO.setAvatarUrl(user.getAvatarUrl());
            loginVO.setToken(token);
            return loginVO;

        } catch (Exception e) {
            // 认证失败（密码错误 / 用户不存在）时捕获异常，统一抛出业务异常（用户名或密码错误）
            throw new RuntimeException("用户名或密码错误");
        }
    }

    /**
     * 用户登出
     * 当前为无状态 JWT 方案，服务端无需操作
     * 若后续需要实现 Token 黑名单，可在此处将 Token 存入 Redis
     * 并设置与剩余有效期相同的 TTL
     */
    public void logout() {
        // 清空 SecurityContext
        SecurityContextHolder.clearContext();
        // 预留：如果需要实现 Token 黑名单，此处将 token 存入 Redis 并设置过期时间
    }

    /**
     * 根据 userId 查询用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    public UserInfoVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        // 用户不存在则抛出业务异常（用户不存在）
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return convertToUserInfoVO(user);
    }

    /**
     * 从 SecurityContextHolder 中获取当前登录用户的 userId
     * 供其他模块（如健身助手模块）调用，避免重复解析 Token
     *
     * @return 当前登录用户ID，如果未登录返回 null
     */
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String username = authentication.getName();
        if (username == null || "anonymousUser".equals(username)) {
            return null;
        }
        // 根据 username 查询 userId
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(queryWrapper);
        return user != null ? user.getId() : null;
    }

    /**
     * User 实体转 UserInfoVO
     * 将毫秒时间戳转换为 LocalDateTime
     */
    private UserInfoVO convertToUserInfoVO(User user) {
        UserInfoVO vo = new UserInfoVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        if (user.getCreatedAt() != null) {
            Instant instant = Instant.ofEpochMilli(user.getCreatedAt());
            vo.setCreatedAt(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
        }
        return vo;
    }
}
