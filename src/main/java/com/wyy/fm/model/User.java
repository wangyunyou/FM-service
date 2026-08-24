package com.wyy.fm.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_openid", columnList = "openid", unique = true),
        @Index(name = "idx_phone", columnList = "phone")
})
public class User extends BaseEntity {

    /** 微信 openid */
    @Column(nullable = false, unique = true, length = 64)
    private String openid;

    /** 微信 unionId（可选，绑定开放平台时有） */
    @Column(length = 64)
    private String unionId;

    /** 昵称 */
    @Column(length = 64)
    private String nickname;

    /** 头像 URL */
    @Column(length = 512)
    private String avatarUrl;

    /** 手机号 */
    @Column(length = 20)
    private String phone;

    /** 性别 0未知 1男 2女 */
    @Column(columnDefinition = "smallint default 0")
    private Integer gender;

    /** 状态 0正常 1禁用 */
    @Column(columnDefinition = "smallint default 0")
    private Integer status;
}
