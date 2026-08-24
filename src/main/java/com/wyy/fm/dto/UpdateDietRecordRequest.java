package com.wyy.fm.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 更新饮食记录请求
 */
@Data
public class UpdateDietRecordRequest {

    /** 餐次：1早餐 2午餐 3晚餐 4加餐 */
    @Min(value = 1, message = "餐次范围 1-4")
    @Max(value = 4, message = "餐次范围 1-4")
    private Integer mealType;

    private String foodName;

    @Min(value = 0, message = "热量不能为负数")
    private Integer calories;

    private String remark;
}
