package com.wyy.fm.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
     * - @Size(256)：限制异常长的入参，避免白白消耗内存
     */
    @NotBlank(message = "code 不能为空")
    @Size(max = 256, message = "code 过长")
    private String code;

    /**
     * 用户昵称（可选，首次登录时传）
     * - 如果用户授权了昵称/头像，前端会一起传过来
     * - 后端自动保存到用户表
     * - 长度上限对齐 users 表字段定义，避免微信默认昵称超长写库失败
     */
    @Size(max = 64, message = "昵称最长 64 个字符")
    private String nickname;

    /**
     * 头像 URL（可选）
     * - 长度上限对齐 users.avatar_url（VARCHAR(512)）
     */
    @Size(max = 512, message = "头像 URL 最长 512 个字符")
    private String avatarUrl;

    /**
     * 性别（可选）
     * - 0：未知 / 1：男 / 2：女，与 User.gender 枚举语义一致
     */
    @Min(value = 0, message = "性别只能为 0/1/2")
    @Max(value = 2, message = "性别只能为 0/1/2")
    private Integer gender;
}
