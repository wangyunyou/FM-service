package com.wyy.fm.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 饮食记录响应 DTO
 * 
 * 作用：返回给前端的饮食记录数据
 * 
 * 示例：
 * {
 *   "id": 1,
 *   "recordDate": "2026-08-24",
 *   "mealType": 1,
 *   "mealTypeName": "早餐",
 *   "foodName": "鸡蛋",
 *   "calories": 70,
 *   "remark": "水煮蛋",
 *   "createdAt": "2026-08-24T08:30:00"
 * }
 * 
 * 注解说明：
 * - @Builder：支持链式构建对象
 */
@Data
@Builder
public class DietRecordResponse {

    /**
     * 记录 ID
     */
    private Long id;

    /**
     * 记录日期
     */
    private LocalDate recordDate;

    /**
     * 餐次（数字）
     * - 1：早餐
     * - 2：午餐
     * - 3：晚餐
     * - 4：加餐
     */
    private Integer mealType;

    /**
     * 餐次名称（中文）
     * - 前端可以直接展示，不用自己转换
     */
    private String mealTypeName;

    /**
     * 食物名称
     */
    private String foodName;

    /**
     * 热量（千卡）
     */
    private Integer calories;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     * - LocalDateTime：包含日期和时间
     */
    private LocalDateTime createdAt;

    /**
     * 餐次数字 → 中文名称 转换方法
     * 
     * 使用示例：
     * String name = DietRecordResponse.getMealTypeName(1);  // 返回 "早餐"
     * 
     * 语法说明：
     * - 这是 Java 14+ 的 switch 表达式语法（箭头函数形式）
     * - 比传统的 switch-case 更简洁
     */
    public static String getMealTypeName(Integer mealType) {
        if (mealType == null) return "未知";
        return switch (mealType) {
            case 1 -> "早餐";
            case 2 -> "午餐";
            case 3 -> "晚餐";
            case 4 -> "加餐";
            default -> "未知";
        };
    }
}
