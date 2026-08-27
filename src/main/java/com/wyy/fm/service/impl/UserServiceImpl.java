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
import org.springframework.dao.DataIntegrityViolationException;
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

    /**
     * 账号状态：正常
     * - 与 User.status 的注释保持同一口径（0 正常 / 1 禁用）
     */
    private static final int USER_STATUS_NORMAL = 0;

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
     * 3. 校验账号状态（禁用账号不给发 token）
     * 4. 仅本次新建的账号接受初始资料（避免刷掉用户自己改过的昵称）
     * 5. 生成 JWT token
     */
    @Override
    @Transactional
    public LoginResponse wxLogin(WxLoginRequest request) {
        // 1. 调用微信接口，用 code 换取 openid
        String openid = wxApiService.code2Session(request.getCode());
        if (openid == null || openid.isBlank()) {
            // 微信在参数异常时可能既不返 errcode 也不返 openid，
            // 不拦住就会往下建出一个 openid 为空的脏账号（靠 nullable=false 报 500）
            throw new BusinessException(ErrorCode.WX_LOGIN_FAILED, "微信未返回 openid");
        }

        // 2. 根据 openid 查找用户；isNewUser 只以服务端这次有没有真的建号为准
        boolean isNewUser = false;
        User user = userRepository.findByOpenid(openid).orElse(null);

        if (user == null) {
            // 用户不存在 → 自动注册
            user = new User();
            user.setOpenid(openid);
            user.setStatus(0);  // 默认正常状态
            isNewUser = true;
            log.info("新用户注册: openid={}", openid);
        } else if (user.getStatus() != null && user.getStatus() != USER_STATUS_NORMAL) {
            // 老用户被禁用：不再发新 token（已发出去的 token 由读接口 getActiveUserById 拦住）
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        // 与客户端上报的标记对账（不参与决策，只用于发现前端逻辑跑偏）：
        // 客户端那个标记不可靠 —— token 被清（401/1002/退出登录）后，老用户重登时
        // 前端的 isFirstLogin() 也会返回 true，于是又绕回"默认昵称刷掉自定义昵称"。
        if (isNewUser && Boolean.FALSE.equals(request.getIsNewUser())) {
            log.warn("客户端上报 isNewUser=false，但服务端本次确实新建了账号，按服务端结果处理: openid={}", openid);
        }
        if (!isNewUser && Boolean.TRUE.equals(request.getIsNewUser())) {
            // 老客户端不发该字段（缺省 true），会稳定走到这里，属预期，用 debug 避免刷日志
            log.debug("客户端上报 isNewUser=true，但账号已存在，忽略其初始资料: openid={}", openid);
        }

        // 3. 初始资料：只给「本次真的新建了账号」的用户写。
        //    微信现在的 getUserProfile 只能拿到固定默认值（"微信用户" + 灰色头像），
        //    不加这个判定，老用户每次重登都会被刷回默认昵称，盖掉自己在「我的」页改的名字（实测复现过）。
        if (isNewUser) {
            if (request.getNickname() != null) {
                user.setNickname(trimToNull(request.getNickname()));
            }
            if (request.getAvatarUrl() != null) {
                user.setAvatarUrl(trimToNull(request.getAvatarUrl()));
            }
            if (request.getGender() != null) {
                user.setGender(request.getGender());
            }
        }

        // 4. 保存用户（插入或更新）
        //    并发首登（同一 openid 两个请求同时进来）会两方都查到空并都去 insert，
        //    靠 openid 唯一索引兜底；接住冲突改为重新查找已存在账号，用户看到的是“登录成功”而不是 500
        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            User existing = userRepository.findByOpenid(openid).orElseThrow(() -> ex);
            log.warn("openid 并发注册冲突，改用已存在用户: userId={}", existing.getId());
            if (existing.getStatus() != null && existing.getStatus() != USER_STATUS_NORMAL) {
                throw new BusinessException(ErrorCode.USER_DISABLED);
            }
            user = existing;
            isNewUser = false;   // 冲突说明账号本来就在，按老用户处理（不再写初始资料）
        }

        // 5. 生成 JWT token
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
     *
     * 这里用 getActiveUserById 而不是 getById：被禁用的账号拿着未过期的旧 token
     * 也不应该继续读到个人数据，返回 1002 后前端 request 层会清 token 并退回登录页。
     */
    @Override
    public UserInfoResponse getUserInfo(Long userId) {
        // 1. 查询用户并校验状态（不存在 1001 / 已禁用 1002）
        User user = getActiveUserById(userId);
        
        // 2. 转换为 DTO（不暴露敏感字段）
        UserInfoResponse response = new UserInfoResponse();
        response.setId(user.getId());
        response.setNickname(user.getNickname());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setPhone(user.getPhone());
        response.setGender(user.getGender());
        response.setStatus(user.getStatus());
        return response;
    }

    /**
     * 更新用户信息实现
     * 
     * @Transactional：事务注解，保证更新操作的原子性
     *
     * 部分更新口径：null / 缺键 = 不改该字段（字符串字段传空串会被 @NotBlank 挡住，
     * 所以昵称/头像不存在“刷成空”这个取直途径，只能用一个新值覆盖）。
     * 写入前统一 trim，避免带尾随空格的值落库后在列表里对不齐。
     */
    @Override
    @Transactional
    public void updateUser(Long userId, UpdateUserRequest request) {
        // 1. 查询用户（禁用账号不能改资料）
        User user = getActiveUserById(userId);
        
        // 2. 部分更新（只更新传入的字段）
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname().trim());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
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

    /**
     * 根据 ID 查询用户并校验账号状态
     */
    @Override
    public User getActiveUserById(Long userId) {
        User user = getById(userId);
        if (user.getStatus() != null && user.getStatus() != USER_STATUS_NORMAL) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        return user;
    }

    /**
     * 把空白串归一为 null，避免微信默认的空昵称以“一串空格”的形式落库
     */
    private static String trimToNull(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
