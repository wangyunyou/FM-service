package com.wyy.fm.service.impl;

import com.wyy.fm.common.JwtUtil;
import com.wyy.fm.dto.*;
import com.wyy.fm.model.User;
import com.wyy.fm.repository.UserRepository;
import com.wyy.fm.service.UserService;
import com.wyy.fm.service.WxApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final WxApiService wxApiService;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public LoginResponse wxLogin(WxLoginRequest request) {
        // 1. code 换 openid
        String openid = wxApiService.code2Session(request.getCode());

        // 2. 查找或创建用户
        boolean isNewUser = false;
        User user = userRepository.findByOpenid(openid).orElse(null);
        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setStatus(0);
            isNewUser = true;
        }

        // 3. 更新用户信息（如果前端传了）
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }

        user = userRepository.save(user);

        // 4. 生成 JWT
        String token = jwtUtil.generateToken(user.getId());

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .isNewUser(isNewUser)
                .build();
    }

    @Override
    public UserInfoResponse getUserInfo(Long userId) {
        User user = getById(userId);
        UserInfoResponse response = new UserInfoResponse();
        response.setId(user.getId());
        response.setNickname(user.getNickname());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setPhone(user.getPhone());
        response.setGender(user.getGender());
        return response;
    }

    @Override
    @Transactional
    public void updateUser(Long userId, UpdateUserRequest request) {
        User user = getById(userId);
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        userRepository.save(user);
    }

    @Override
    public User getById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }
}
