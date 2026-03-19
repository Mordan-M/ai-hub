package com.mordan.aihub.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mordan.aihub.auth.util.JwtTokenProvider;
import com.mordan.aihub.common.utils.ResultUtils;
import com.mordan.aihub.common.vo.BaseResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 * 从请求头中提取 Token 并验证，设置认证信息到 SecurityContext
 *
 * @author auth
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   UserDetailsService userDetailsService,
                                   ObjectMapper objectMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. 从请求头 Authorization 中取 Bearer Token
            String token = getTokenFromRequest(request);

            // 2. Token 不存在或格式不正确则直接放行
            if (!StringUtils.hasText(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 3. 调用 JwtTokenProvider.validateToken() 校验
            if (!jwtTokenProvider.validateToken(token)) {
                // 校验失败，清空 SecurityContext，返回 401
                SecurityContextHolder.clearContext();
                sendUnauthorizedResponse(response);
                return;
            }

            // 4. 校验通过则解析 userId 和 username，构建 Authentication 写入 SecurityContext
            Long userId = jwtTokenProvider.getUserId(token);
            String username = jwtTokenProvider.getUsername(token);

            // 加载用户详情
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // 构建认证令牌
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // 写入 SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 继续过滤链
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            // 发生异常，清空上下文，返回 401
            SecurityContextHolder.clearContext();
            sendUnauthorizedResponse(response);
        }
    }

    /**
     * 从请求头中提取 Bearer Token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 发送 401 响应，使用项目统一返回格式
     */
    private void sendUnauthorizedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        BaseResponse<Void> baseResponse = ResultUtils.error(401, "Token 已过期或无效");
        response.getWriter().write(objectMapper.writeValueAsString(baseResponse));
    }
}
