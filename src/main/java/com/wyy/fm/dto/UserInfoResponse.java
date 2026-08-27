package com.wyy.fm.dto;

import lombok.Data;

/**
 * 用户信息响应 DTO
 * 
 * 作用：前端调用 GET /api/user/info 时返回的用户信息
 * 
 * 示例：
 * {
 *   "code": 200,
 *   "data": {
 *     "id": 1,
 *     "nickname": "张三",
 *     "avatarUrl": "https://...",
 *     "phone": "13800138000",
 *     "gender": 1
 *   }
 * }
 * 
 * 注意：
 * - 不返回 openid、unionId 等敏感信息
 * - 只返回前端展示需要的字段
 */
@Data
public class UserInfoResponse {

    /**
     * 用户 ID
     */
    private Long id;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像 URL
     */
    private String avatarUrl;

    /**
     * 手机号（可能为 null）
     */
    private String phone;

    /**
     * 性别
     * - 0：未知
     * - 1：男
     * - 2：女
     */
    private Integer gender;

    /**
     * 账号状态
     * - 0：正常
     * - 1：禁用
     *
     * 为什么能读到：本接口已经拦住了 status=1 的账号（返回 1002），
     * 能走到这里的必然是 0；保留该字段是为了给前端做“非正常就不弹详情”的兼容判定，
     * 以后如果开放自助申诉/封禁提示不需要再改接口。
     */
    private Integer status;
}
