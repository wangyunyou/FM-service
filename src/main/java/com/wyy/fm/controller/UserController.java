package com.wyy.fm.controller;

import com.wyy.fm.common.Result;
import com.wyy.fm.config.AuthInterceptor;
import com.wyy.fm.dto.*;
import com.wyy.fm.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 微信登录（无需 token）
     */
    @PostMapping("/wx-login")
    public Result<LoginResponse> wxLogin(@Valid @RequestBody WxLoginRequest request) {
        return Result.ok(userService.wxLogin(request));
    }

    /**
     * 获取当前用户信息（需要 token）
     */
    @GetMapping("/info")
    public Result<UserInfoResponse> getUserInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.CURRENT_USER_ID);
        return Result.ok(userService.getUserInfo(userId));
    }

    /**
     * 更新用户信息（需要 token）
     */
    @PutMapping("/info")
    public Result<Void> updateUser(
            HttpServletRequest request,
            @RequestBody UpdateUserRequest updateRequest) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.CURRENT_USER_ID);
        userService.updateUser(userId, updateRequest);
        return Result.ok();
    }
}
