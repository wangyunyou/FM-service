package com.wyy.fm.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 饮食记录实体 — 对应数据库表 diet_records
 * 
 * 作用：记录用户每餐吃了什么、摄入了多少热量
 * 
 * 关联关系：
 * - userId 字段关联 users 表的 id（逻辑外键，没用 JPA 的 @ManyToOne）
 * - 一个用户可以有多条饮食记录（一对多）
 * 
 * 索引说明：
 * - idx_user_date：联合索引（user_id + record_date），用于快速查询某用户某天的记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "diet_records", indexes = {
        @Index(name = "idx_user_date", columnList = "user_id, record_date")  // 联合索引：查询某用户某天记录时用
})
public class DietRecord extends BaseEntity {

    /**
     * 所属用户 ID
     * - 关联 users 表的 id
     * - 不用 @ManyToOne 是因为项目简单，手动管理关联关系
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 记录日期
     * - LocalDate：只存日期，不存时间（如 2026-08-24）
     * - 用于按天查询饮食记录
     */
    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    /**
     * 餐次
     * - 1：早餐
     * - 2：午餐
     * - 3：晚餐
     * - 4：加餐（零食、饮料等）
     */
    @Column(nullable = false)
    private Integer mealType;

    /**
     * 食物名称
     * - 如："鸡蛋"、"全麦面包"、"牛奶"
     * - length = 200：限制长度
     */
    @Column(name = "food_name", nullable = false, length = 200)
    private String foodName;

    /**
     * 热量（千卡/kcal）
     * - 如：鸡蛋 70kcal，全麦面包 80kcal
     */
    @Column(nullable = false)
    private Integer calories;

    /**
     * 备注
     * - 可选字段，记录额外信息（如"加了糖"、"无糖版本"）
     */
    @Column(length = 500)
    private String remark;
}
