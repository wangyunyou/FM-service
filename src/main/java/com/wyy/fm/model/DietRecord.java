package com.wyy.fm.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 饮食记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "diet_records", indexes = {
        @Index(name = "idx_user_date", columnList = "user_id, record_date")
})
public class DietRecord extends BaseEntity {

    /** 所属用户 */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 记录日期 */
    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    /** 餐次：1早餐 2午餐 3晚餐 4加餐 */
    @Column(nullable = false)
    private Integer mealType;

    /** 食物名称 */
    @Column(name = "food_name", nullable = false, length = 200)
    private String foodName;

    /** 热量（千卡） */
    @Column(nullable = false)
    private Integer calories;

    /** 备注 */
    @Column(length = 500)
    private String remark;
}
