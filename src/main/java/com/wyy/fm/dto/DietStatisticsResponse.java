package com.wyy.fm.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 饮食统计响应 DTO
 * 
 * 作用：前端调用 GET /api/diet/query 时返回的统计数据
 * 
 * 示例：
 * {
 *   "totalCalories": 1800,
 *   "caloriesByMeal": {
 *     "早餐": 400,
 *     "午餐": 700,
 *     "晚餐": 600,
 *     "加餐": 100
 *   },
 *   "recordCount": 12,
 *   "avgCaloriesPerDay": 600,
 *   "records": [
 *     {"id": 1, "foodName": "鸡蛋", ...},
 *     {"id": 2, "foodName": "牛奶", ...}
 *   ]
 * }
 */
@Data
@Builder
public class DietStatisticsResponse {

    /**
     * 总热量（千卡）
     * - 查询日期范围内所有记录的总和
     */
    private Integer totalCalories;

    /**
     * 按餐次统计的热量
     * - Key：餐次名称（"早餐"、"午餐"、"晚餐"、"加餐"）
     * - Value：该餐次的总热量
     * - Map：键值对集合
     */
    private Map<String, Integer> caloriesByMeal;

    /**
     * 记录条数
     * - 查询日期范围内有多少条饮食记录
     */
    private Integer recordCount;

    /**
     * 日均热量
     * - 总热量 / 天数
     * - 用于前端展示"平均每天摄入多少热量"
     */
    private Integer avgCaloriesPerDay;

    /**
     * 明细列表
     * - 查询日期范围内的所有饮食记录
     * - List<DietRecordResponse>：DietRecordResponse 对象的列表
     */
    private List<DietRecordResponse> records;
}
