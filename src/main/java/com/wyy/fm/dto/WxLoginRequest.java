package com.wyy.fm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信登录请求 DTO
 * 
 * 作用：前端调用 POST /api/user/wx-login 时传入的请求体
 * 
 * 流程：
 * 1. 小程序调用 wx.login() 获取临时登录凭证 code
 * 2. 前端将 code 传给后端
 * 3. 后端用 code 调用微信接口换取 openid
 * 4. 根据 openid 查找或创建用户，返回 JWT token
 * 
 * 示例：
 * {"code": "0b00H1ll2xxxxX...", "nickname": "张三", "avatarUrl": "https://..."}
 */
@Data
public class WxLoginRequest {

    /**
     * 微信登录凭证
     * - 由小程序 wx.login() 获取
     * - 一次性使用，有效期 5 分钟
     * - @NotBlank：不能为空或空字符串
     */
    @NotBlank(message = "code 不能为空")
    private String code;

    /**
     * 用户信息（可选，首次登录时传）
     * - 如果用户授权了昵称/头像，前端会一起传过来
     * - 后端自动保存到用户表
     */
    private String nickname;
    private String avatarUrl;
    private Integer gender;
}
