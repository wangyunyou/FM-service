package com.wyy.fm.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 基础实体类 — 所有实体的父类
 * 
 * 作用：
 * 1. 统一提供 id 主键（自增）
 * 2. 自动维护创建时间和更新时间（不用手动 set）
 * 
 * 注解说明：
 * - @Data：Lombok 注解，自动生成 getter/setter/toString/equals/hashCode
 * - @MappedSuperclass：JPA 注解，标记为"映射父类"，字段会被子类继承到数据库表
 * - abstract：抽象类，不能直接实例化，只能被继承
 */
@Data
@MappedSuperclass
public abstract class BaseEntity {

    /**
     * 主键 ID
     * - @Id：标记为主键
     * - @GeneratedValue(strategy = IDENTITY)：数据库自增（MySQL/PostgreSQL/H2 都支持）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 创建时间
     * - @CreationTimestamp：Hibernate 注解，插入时自动填充当前时间
     * - updatable = false：不允许更新（保护字段）
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     * - @UpdateTimestamp：Hibernate 注解，每次更新时自动填充当前时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
