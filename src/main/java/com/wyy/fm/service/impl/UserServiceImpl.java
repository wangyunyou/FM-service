package com.wyy.fm.service.impl;

import com.wyy.fm.common.BusinessException;
import com.wyy.fm.common.ErrorCode;
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

/**
 * 用户服务实现类
 * 
 * 注解说明：
 * - @Service：标记为服务层组件，Spring 自动注册为 Bean
 * - @Slf4j：Lombok 注解，自动生成 log 对象（日志工具）
 * - @RequiredArgsConstructor：自动生成构造函数，注入 final 字段
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    // 依赖注入（构造函数注入）
    private final UserRepository userRepository;  // 用户数据访问
    private final WxApiService wxApiService;      // 微信 API 调用
    private final JwtUtil jwtUtil;                // JWT 工具

    /**
     * 微信登录实现
     * 
     * @Transactional：事务注解，保证方法内的数据库操作要么全成功，要么全回滚
     * 
     * 流程：
     * 1. 用 code 换 openid
     * 2. 查找或创建用户
     * 3. 更新用户信息
     * 4. 生成 JWT token
     */
    @Override
    @Transactional
    public LoginResponse wxLogin(WxLoginRequest request) {
        // 1. 调用微信接口，用 code 换取 openid
        String openid = wxApiService.code2Session(request.getCode());

        // 2. 根据 openid 查找用户
        boolean isNewUser = false;
        User user = userRepository.findByOpenid(openid).orElse(null);
        
        // 如果用户不存在，创建新用户
        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setStatus(0);  // 默认正常状态
            isNewUser = true;
            log.info("新用户注册: openid={}", openid);  // 日志记录
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

        // 保存用户（插入或更新）
        user = userRepository.save(user);

        // 4. 生成 JWT token
        String token = jwtUtil.generateToken(user.getId());

        // 构建响应对象（使用 Builder 模式）
        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .isNewUser(isNewUser)
                .build();
    }

    /**
     * 获取用户信息实现
     */
    @Override
    public UserInfoResponse getUserInfo(Long userId) {
        // 1. 查询用户（不存在会抛异常）
        User user = getById(userId);
        
        // 2. 转换为 DTO（不暴露敏感字段）
        UserInfoResponse response = new UserInfoResponse();
        response.setId(user.getId());
        response.setNickname(user.getNickname());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setPhone(user.getPhone());
        response.setGender(user.getGender());
        return response;
    }

    /**
     * 更新用户信息实现
     * 
     * @Transactional：事务注解，保证更新操作的原子性
     */
    @Override
    @Transactional
    public void updateUser(Long userId, UpdateUserRequest request) {
        // 1. 查询用户
        User user = getById(userId);
        
        // 2. 部分更新（只更新传入的字段）
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        
        // 3. 保存
        userRepository.save(user);
    }

    /**
     * 根据 ID 查询用户
     * 
     * @throws BusinessException 如果用户不存在，抛出 USER_NOT_FOUND 异常
     */
    @Override
    public User getById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
