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
 *
 * 关于初始资料（nickname / avatarUrl / gender）：
 * - 这三项只在「本次服务端真的新建了账号」时被写入，判据是服务端的 isNewUser，
 *   不是客户端上报的 request.isNewUser（后者仅用于对账日志）
 * - 为什么必须这样：微信现在的 getUserProfile 只能拿到固定默认值（"微信用户" + 灰色头像），
 *   老用户每次重登都照单全收的话，会把自己在「我的」页改过的名字刷回默认值（实测复现过）
 * - 前端因此不需要在登录时维护资料，改昵称/性别请走 PUT /api/user/info
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

    /**
     * 客户端自报的「本次是否首次注册」标记。
     *
     * 重要：后端**不以它为准**。是否写入上面的 nickname / avatarUrl / gender，
     * 只看 UserServiceImpl 里服务端自己算出的 isNewUser（本次有没有真的建号）。
     * 原因：这个标记在客户端不可靠 —— token 被清（401/1002/退出登录）后，
     * 老用户重新登录时前端也会判定成"首登"，于是又绕回被微信默认昵称刷掉的问题。
     *
     * 那为什么还留着：
     * - 服务端会拿它跟自己算的结果对账并打日志，前端逻辑跑偏时能第一时间发现
     * - 响应体 LoginResponse.isNewUser 才是权威值，客户端应以它为准更新本地状态
     *
     * 缺省 true：与老客户端（不发该字段）的行为保持一致，不影响新用户对账。
     */
    private Boolean isNewUser = Boolean.TRUE;
}
