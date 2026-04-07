package com.mordan.aihub.auth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mordan.aihub.auth.domain.vo.LoginRequest;
import com.mordan.aihub.auth.domain.vo.LoginVO;
import com.mordan.aihub.auth.domain.vo.RegisterRequest;
import com.mordan.aihub.auth.domain.vo.UserInfoVO;
import com.mordan.aihub.auth.service.UserService;
import com.mordan.aihub.auth.util.JwtTokenProvider;
import com.mordan.aihub.fitness.domain.entity.User;
import com.mordan.aihub.fitness.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Value;
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
 * 用户业务服务实现类
 *
 * @author auth
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${auth.register.enable:false}")
    private boolean registerEnabled;

    public UserServiceImpl(PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtTokenProvider jwtTokenProvider) {
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public UserInfoVO register(RegisterRequest request) {
        // 检查注册开关是否开启
        if (!registerEnabled) {
            throw new RuntimeException("注册功能已关闭，请联系管理员开启");
        }
        // 1. 检查 username 是否已存在，存在则抛出业务异常（用户名已被占用）
        User existUser = this.lambdaQuery()
                .eq(User::getUsername, request.username())
                .one();
        if (existUser != null) {
            throw new RuntimeException("用户名已被占用");
        }

        // 2. 密码用 BCryptPasswordEncoder 加密后存入 users 表
        // 3. nickname 若未填写，默认等于 username
        String nickname = request.nickname();
        if (nickname == null || nickname.isBlank()) {
            nickname = request.username();
        }

        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .nickname(nickname)
                .avatarUrl("https://img2.baidu.com/it/u=735961080,1059579988&fm=253&fmt=auto&app=120&f=JPEG?w=500&h=662")
                .status((byte) 1)
                .build();

        this.save(user);

        // 4. 返回注册成功的 UserInfoVO
        return convertToUserInfoVO(user);
    }

    @Override
    public LoginVO login(LoginRequest request) {
        try {
            // 1. 调用 AuthenticationManager.authenticate() 触发 Spring Security 认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );

            // 2. 认证成功
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 根据用户名查询用户信息
            User user = this.lambdaQuery()
                    .eq(User::getUsername, request.username())
                    .one();

            // 3. 生成 Token
            String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());

            // 4. 返回 LoginVO
            return new LoginVO(
                    user.getId(),
                    user.getUsername(),
                    user.getNickname(),
                    user.getAvatarUrl(),
                    token
            );

        } catch (Exception e) {
            // 认证失败（密码错误 / 用户不存在）时捕获异常，统一抛出业务异常（用户名或密码错误）
            throw new RuntimeException("用户名或密码错误");
        }
    }

    @Override
    public void logout() {
        // 清空 SecurityContext
        SecurityContextHolder.clearContext();
        // 预留：如果需要实现 Token 黑名单，此处将 token 存入 Redis 并设置过期时间
    }

    @Override
    public UserInfoVO getUserInfo(Long userId) {
        User user = this.getById(userId);
        // 用户不存在则抛出业务异常（用户不存在）
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return convertToUserInfoVO(user);
    }

    @Override
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
        User user = this.lambdaQuery()
                .eq(User::getUsername, username)
                .one();
        return user != null ? user.getId() : null;
    }

    /**
     * User 实体转 UserInfoVO
     * 将毫秒时间戳转换为 LocalDateTime
     */
    private UserInfoVO convertToUserInfoVO(User user) {
        LocalDateTime createdAt = null;
        if (user.getCreatedAt() != null) {
            Instant instant = Instant.ofEpochMilli(user.getCreatedAt());
            createdAt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        }
        return new UserInfoVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarUrl(),
                createdAt
        );
    }
}
