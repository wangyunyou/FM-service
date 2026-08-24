package com.wyy.fm.service;

import com.wyy.fm.dto.*;
import com.wyy.fm.model.User;

public interface UserService {

    /**
     * 微信登录：code 换 openid，自动注册/更新用户，返回 JWT
     */
    LoginResponse wxLogin(WxLoginRequest request);

    /**
     * 获取当前用户信息
     */
    UserInfoResponse getUserInfo(Long userId);

    /**
     * 更新用户信息
     */
    void updateUser(Long userId, UpdateUserRequest request);

    /**
     * 根据 ID 查用户（内部用）
     */
    User getById(Long userId);
}
