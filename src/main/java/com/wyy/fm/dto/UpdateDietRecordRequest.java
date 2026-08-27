package com.wyy.fm.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新饮食记录请求 DTO
 * 
 * 作用：前端调用 PUT /api/diet/{id} 时传入的请求体
 * 
 * 特点：
 * - 所有字段都是可选的（没有 @NotNull）
 * - 只更新传入的字段，没传的字段保持不变
 * - 这叫"部分更新"（Partial Update）
 * 
 * 示例：
 * 只修改食物名称：{"foodName": "燕麦片"}
 * 只修改热量：{"calories": 150}
 * 同时修改多个：{"foodName": "燕麦片", "calories": 150}
 */
@Data
public class UpdateDietRecordRequest {

    /**
     * 餐次（可选）
     * - 传了就更新，没传保持不变
     */
    @Min(value = 1, message = "餐次范围 1-4")
    @Max(value = 4, message = "餐次范围 1-4")
    private Integer mealType;

    /**
     * 食物名称（可选）
     * - @Size(min=1)：传了就不能是空串（想置空请传 null，表示不改）
     * - 长度上限对齐 diet_records.food_name（VARCHAR(200)）
     */
    @Size(min = 1, max = 200, message = "食物名称长度 1-200 个字符")
    private String foodName;

    /**
     * 热量（可选）
     */
    @Min(value = 0, message = "热量不能为负数")
    private Integer calories;

    /**
     * 备注（可选）
     * - 长度上限对齐 diet_records.remark（VARCHAR(500)）
     */
    @Size(max = 500, message = "备注最长 500 个字符")
    private String remark;
}
