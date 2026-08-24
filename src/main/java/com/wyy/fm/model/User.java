package com.wyy.fm.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体 — 对应数据库表 users
 * 
 * 作用：存储小程序用户信息（微信 openid、昵称、头像、手机号等）
 * 
 * 注解说明：
 * - @Entity：JPA 注解，标记为实体类，会自动映射到数据库表
 * - @Table：指定表名和索引
 * - @EqualsAndHashCode(callSuper = true)：equals/hashCode 比较时包含父类字段（id、createdAt、updatedAt）
 * 
 * 继承关系：
 * User → BaseEntity（继承 id、createdAt、updatedAt）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_openid", columnList = "openid", unique = true),  // openid 唯一索引（查询用）
        @Index(name = "idx_phone", columnList = "phone")                     // 手机号普通索引（可选）
})
public class User extends BaseEntity {

    /**
     * 微信 openid
     * - 每个用户在每个小程序下都有唯一的 openid
     * - 用于微信登录时识别用户身份
     * - nullable = false：不能为空
     * - unique = true：数据库层面保证唯一
     * - length = 64：字段长度限制
     */
    @Column(nullable = false, unique = true, length = 64)
    private String openid;

    /**
     * 微信 unionId（可选）
     * - 如果小程序绑定了微信开放平台，可以拿到 unionId
     * - unionId 在同一开放平台下的所有应用中唯一
     * - 用于跨应用识别同一用户
     */
    @Column(length = 64)
    private String unionId;

    /**
     * 昵称
     * - 用户可以在小程序内修改
     */
    @Column(length = 64)
    private String nickname;

    /**
     * 头像 URL
     * - 可以是微信头像或用户上传的自定义头像
     */
    @Column(length = 512)
    private String avatarUrl;

    /**
     * 手机号
     * - 可选字段，用户授权后获取
     * - 用于手机号登录或绑定
     */
    @Column(length = 20)
    private String phone;

    /**
     * 性别
     * - 0：未知（默认）
     * - 1：男
     * - 2：女
     * - columnDefinition：指定数据库字段类型
     */
    @Column(columnDefinition = "smallint default 0")
    private Integer gender;

    /**
     * 账号状态
     * - 0：正常（默认）
     * - 1：禁用（被封禁）
     * - 用于控制用户是否可以使用系统
     */
    @Column(columnDefinition = "smallint default 0")
    private Integer status;
}
