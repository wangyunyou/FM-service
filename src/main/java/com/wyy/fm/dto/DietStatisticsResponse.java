package com.wyy.fm.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 饮食统计响应
 */
@Data
@Builder
public class DietStatisticsResponse {

    /** 总热量 */
    private Integer totalCalories;

    /** 按餐次统计 */
    private Map<String, Integer> caloriesByMeal;

    /** 记录条数 */
    private Integer recordCount;

    /** 日均热量 */
    private Integer avgCaloriesPerDay;

    /** 明细列表 */
    private List<DietRecordResponse> records;
}
