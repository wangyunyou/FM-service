package com.wyy.fm.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 登录响应 DTO
 * 
 * 作用：前端调用 POST /api/user/wx-login 成功后返回的数据
 * 
 * 示例：
 * {
 *   "code": 200,
 *   "data": {
 *     "token": "eyJhbGciOiJIUzI1NiJ9...",
 *     "userId": 1,
 *     "nickname": "张三",
 *     "avatarUrl": "https://...",
 *     "isNewUser": false
 *   }
 * }
 * 
 * 注解说明：
 * - @Builder：Lombok 注解，支持链式构建对象
 *   例如：LoginResponse.builder().token("xxx").userId(1L).build()
 */
@Data
@Builder
public class LoginResponse {

    /**
     * JWT token
     * - 前端拿到后存到本地存储（localStorage / Storage）
     * - 后续请求放到 Header：Authorization: Bearer {token}
     */
    private String token;

    /**
     * 用户 ID
     * - 数据库主键
     */
    private Long userId;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像 URL
     */
    private String avatarUrl;

    /**
     * 是否新用户
     * - true：首次登录，自动注册
     * - false：老用户，正常登录
     * - 前端可根据这个值决定是否引导用户完善信息
     */
    private Boolean isNewUser;
}
