package com.wyy.fm.dto;

import lombok.Data;

/**
 * 更新用户信息请求 DTO
 * 
 * 作用：前端调用 PUT /api/user/info 时传入的请求体
 * 
 * 特点：
 * - 所有字段都是可选的（部分更新）
 * - 只更新传入的字段，没传的保持不变
 * 
 * 示例：
 * 只修改昵称：{"nickname": "李四"}
 * 同时修改多个：{"nickname": "李四", "gender": 2}
 */
@Data
public class UpdateUserRequest {

    /**
     * 昵称（可选）
     */
    private String nickname;

    /**
     * 头像 URL（可选）
     */
    private String avatarUrl;

    /**
     * 性别（可选）
     * - 0：未知
     * - 1：男
     * - 2：女
     */
    private Integer gender;
}
