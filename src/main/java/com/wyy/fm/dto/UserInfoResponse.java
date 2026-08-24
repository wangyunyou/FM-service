package com.wyy.fm.dto;

import lombok.Data;

/**
 * 用户信息响应
 */
@Data
public class UserInfoResponse {

    private Long id;
    private String nickname;
    private String avatarUrl;
    private String phone;
    private Integer gender;
}
