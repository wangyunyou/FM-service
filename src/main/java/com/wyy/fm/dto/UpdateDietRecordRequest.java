package com.wyy.fm.dto;

import com.wyy.fm.common.NotBlankIfPresent;
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
 *
 * 置空口径：null / 缺键 = 不改；字符串字段传空串 "" = 清空该字段（备注等可选字段常用）
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
     * - @NotBlankIfPresent：传了就不能是空串或纯空白（想「不改」请传 null / 不传该键）
     *   不能用 @NotBlank：它把 null 也判失败，会把"只改热量"这类合法部分更新一起挡掉
     * - 长度上限对齐 diet_records.food_name（VARCHAR(200)）
     */
    @NotBlankIfPresent(message = "食物名称不能为空")
    @Size(max = 200, message = "食物名称最长 200 个字符")
    private String foodName;

    /**
     * 热量（可选）
     * - 上限与前端 constants/validation.ts 的 CALORIES_MAX 同值，改一处必须同步另一处
     */
    @Min(value = 0, message = "热量不能为负数")
    @Max(value = 100000, message = "热量看起来过大（上限 100000）")
    private Integer calories;

    /**
     * 备注（可选）
     * - 长度上限对齐 diet_records.remark（VARCHAR(500)）
     *
     * 置空语义（重要，与前端契约）：
     * - 不传 / 传 null → 保持原备注不变（部分更新）
     * - 传空串 ""     → 清空备注
     * 前端必须发空串而不是省略键，否则“清空”这个意图传不过来。
     */
    @Size(max = 500, message = "备注最长 500 个字符")
    private String remark;
}
