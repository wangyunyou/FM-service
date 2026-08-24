package com.wyy.fm.service;

import com.wyy.fm.dto.*;
import com.wyy.fm.model.User;

/**
 * 用户服务接口
 * 
 * 作用：定义用户相关的业务逻辑
 * 
 * 为什么用接口：
 * 1. 解耦：Controller 只依赖接口，不依赖具体实现
 * 2. 可测试：可以用 Mock 对象替代真实实现
 * 3. 可扩展：可以有多个实现类（如不同版本的登录逻辑）
 * 
 * 实现类：UserServiceImpl
 */
public interface UserService {

    /**
     * 微信登录
     * 
     * 流程：
     * 1. 用 code 调用微信接口换取 openid
     * 2. 根据 openid 查找用户
     * 3. 如果不存在，自动注册新用户
     * 4. 生成 JWT token
     * 5. 返回 token 和用户信息
     * 
     * @param request 登录请求（包含 code、昵称、头像等）
     * @return 登录响应（包含 token、userId、是否新用户等）
     */
    LoginResponse wxLogin(WxLoginRequest request);

    /**
     * 获取当前用户信息
     * 
     * @param userId 当前用户 ID（从 token 解析）
     * @return 用户信息（不包含敏感字段）
     */
    UserInfoResponse getUserInfo(Long userId);

    /**
     * 更新用户信息
     * 
     * @param userId 当前用户 ID
     * @param request 更新请求（部分更新，只更新传入的字段）
     */
    void updateUser(Long userId, UpdateUserRequest request);

    /**
     * 根据 ID 查询用户（内部用）
     * 
     * @param userId 用户 ID
     * @return User 实体（包含所有字段）
     * @throws com.wyy.fm.common.BusinessException 如果用户不存在
     */
    User getById(Long userId);
}
