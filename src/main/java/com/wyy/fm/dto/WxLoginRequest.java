package com.wyy.fm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信登录请求
 */
@Data
public class WxLoginRequest {

    @NotBlank(message = "code 不能为空")
    private String code;

    /** 用户信息（可选，首次登录时传） */
    private String nickname;
    private String avatarUrl;
    private Integer gender;
}
