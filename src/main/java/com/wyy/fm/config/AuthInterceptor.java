package com.wyy.fm.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wyy.fm.common.ErrorCode;
import com.wyy.fm.common.JwtUtil;
import com.wyy.fm.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 认证拦截器
 * 
 * 作用：拦截所有需要登录的接口，验证 JWT token
 * 
 * 流程：
 * 1. 前端请求带 token：Authorization: Bearer {token}
 * 2. 拦截器从 Header 提取 token
 * 3. 验证 token 是否有效（未过期、未篡改）
 * 4. 从 token 解析 userId，存到 request attribute
 * 5. Controller 从 request 取出 userId
 * 
 * 配置位置：
 * - WebMvcConfig.java 里注册拦截器
 * - 拦截 /api/**，排除路径：/api/user/wx-login
 * - 注：/health 不在 /api/** 下，所以不需要排除就能公开访问
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;  // JSON 序列化工具

    /**
     * 常量：request attribute 的 key
     * - 用于在拦截器和 Controller 之间传递当前用户 ID
     */
    public static final String CURRENT_USER_ID = "currentUserId";

    /**
     * 前置拦截方法
     * 
     * 在 Controller 方法执行前调用
     * 
     * @return true：放行，继续执行 Controller
     *         false：拦截，直接返回错误响应
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 从 Header 提取 token
        String token = extractToken(request);

        // 2. 验证 token
        if (token == null || !jwtUtil.validateToken(token)) {
            // token 无效，返回 401
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);  // 401
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(
                    Result.fail(ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getMessage())
            ));
            return false;  // 拦截，不继续执行
        }

        // 3. token 有效，解析 userId 并存到 request attribute
        Long userId = jwtUtil.getUserIdFromToken(token);
        request.setAttribute(CURRENT_USER_ID, userId);
        return true;  // 放行，继续执行 Controller
    }

    /**
     * 从 Authorization Header 提取 token
     * 
     * 格式：Authorization: Bearer {token}
     * 
     * @return token 字符串，或 null（没有 token）
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);  // 去掉 "Bearer " 前缀
        }
        return null;
    }
}
