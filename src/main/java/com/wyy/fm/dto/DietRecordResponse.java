package com.wyy.fm.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 饮食记录响应
 */
@Data
@Builder
public class DietRecordResponse {

    private Long id;
    private LocalDate recordDate;
    private Integer mealType;
    private String mealTypeName;
    private String foodName;
    private Integer calories;
    private String remark;
    private LocalDateTime createdAt;

    /** 餐次名称映射 */
    public static String getMealTypeName(Integer mealType) {
        if (mealType == null) return "未知";
        switch (mealType) {
            case 1: return "早餐";
            case 2: return "午餐";
            case 3: return "晚餐";
            case 4: return "加餐";
            default: return "未知";
        }
    }
}
