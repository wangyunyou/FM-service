package com.wyy.fm.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 新增饮食记录请求
 */
@Data
public class CreateDietRecordRequest {

    /** 记录日期 */
    @NotNull(message = "日期不能为空")
    private java.time.LocalDate recordDate;

    /** 餐次：1早餐 2午餐 3晚餐 4加餐 */
    @NotNull(message = "餐次不能为空")
    @Min(value = 1, message = "餐次范围 1-4")
    @Max(value = 4, message = "餐次范围 1-4")
    private Integer mealType;

    /** 食物名称 */
    @NotBlank(message = "食物名称不能为空")
    private String foodName;

    /** 热量（千卡） */
    @NotNull(message = "热量不能为空")
    @Min(value = 0, message = "热量不能为负数")
    private Integer calories;

    /** 备注 */
    private String remark;
}
