package com.wyy.fm.controller;

import com.wyy.fm.common.Result;
import com.wyy.fm.config.AuthInterceptor;
import com.wyy.fm.dto.*;
import com.wyy.fm.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 * 
 * 接口列表：
 * - POST /api/user/wx-login：微信登录（无需 token）
 * - GET /api/user/info：获取当前用户信息（需要 token）
 * - PUT /api/user/info：更新用户信息（需要 token）
 * 
 * 注解说明：
 * - @RestController：标记为 REST 控制器，返回 JSON
 * - @RequestMapping("/api/user")：统一前缀，所有接口路径都以 /api/user 开头
 * - @RequiredArgsConstructor：Lombok 注解，自动生成构造函数（用于注入 UserService）
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "微信登录、用户信息查询和更新")
public class UserController {

    /**
     * 用户服务（依赖注入）
     * - final：不可变，构造函数注入后不能改
     * - @RequiredArgsConstructor 会自动生成构造函数并注入
     */
    private final UserService userService;

    /**
     * 微信登录接口
     * 
     * 请求：POST /api/user/wx-login
     * 请求体：{"code": "xxx", "nickname": "张三", "avatarUrl": "https://..."}
     * 响应：{"code":200, "data":{"token":"eyJhbG...", "userId":1, "isNewUser":false}}
     * 
     * 注解说明：
     * - @PostMapping("/wx-login")：映射 POST 请求
     * - @Valid：触发 DTO 里的参数校验（@NotBlank 等）
     * - @RequestBody：从请求体 JSON 反序列化为对象
     */
    @PostMapping("/wx-login")
    @Operation(summary = "微信登录", description = "使用微信 code 换取 JWT token，支持自动注册新用户")
    public Result<LoginResponse> wxLogin(@Valid @RequestBody WxLoginRequest request) {
        return Result.ok(userService.wxLogin(request));
    }

    /**
     * 获取当前用户信息接口
     * 
     * 请求：GET /api/user/info
     * Header：Authorization: Bearer {token}
     * 响应：{"code":200, "data":{"id":1, "nickname":"张三", "avatarUrl":"https://..."}}
     * 
     * 流程：
     * 1. AuthInterceptor 拦截请求，验证 token
     * 2. 从 token 解析 userId，存到 request attribute
     * 3. Controller 从 request 取出 userId
     * 4. 调用 Service 查询用户信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取用户信息", description = "获取当前登录用户的基本信息")
    public Result<UserInfoResponse> getUserInfo(HttpServletRequest request) {
        // 从 request attribute 取出当前用户 ID（由 AuthInterceptor 设置）
        Long userId = (Long) request.getAttribute(AuthInterceptor.CURRENT_USER_ID);
        return Result.ok(userService.getUserInfo(userId));
    }

    /**
     * 更新用户信息接口
     * 
     * 请求：PUT /api/user/info
     * Header：Authorization: Bearer {token}
     * 请求体：{"nickname": "李四", "gender": 2}
     * 响应：{"code":200, "message":"success", "data":null}
     */
    @PutMapping("/info")
    @Operation(summary = "更新用户信息", description = "更新当前登录用户的昵称、头像、性别等信息")
    public Result<Void> updateUser(
            HttpServletRequest request,
            @RequestBody UpdateUserRequest updateRequest) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.CURRENT_USER_ID);
        userService.updateUser(userId, updateRequest);
        return Result.ok();  // 更新成功，不返回数据
    }
}
