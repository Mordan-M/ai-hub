package com.mordan.aihub.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mordan.aihub.fitness.domain.entity.User;
import com.mordan.aihub.fitness.mapper.UserMapper;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * Spring Security UserDetailsService 实现
 * 根据用户名查询用户信息
 *
 * @author auth
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    public UserDetailsServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        // 根据 username 查询 users 表
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(queryWrapper);

        // 用户不存在时抛出 UsernameNotFoundException
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 用户 status = 0（禁用）时抛出 DisabledException
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new DisabledException("用户已被禁用: " + username);
        }

        // 返回 UserDetails 对象，暂无角色体系，roles 统一传空列表
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                new ArrayList<>()
        );
    }
}
