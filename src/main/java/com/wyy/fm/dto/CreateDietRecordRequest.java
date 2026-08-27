package com.wyy.fm.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增饮食记录请求 DTO
 * 
 * 作用：前端调用 POST /api/diet 时传入的请求体
 * 
 * 参数校验注解：
 * - @NotNull：不能为 null（数字类型）
 * - @NotBlank：不能为 null 且不能为空字符串（字符串类型）
 * - @Min / @Max：数值范围限制
 * - message：校验失败时返回的错误信息
 * 
 * 使用方式：
 * Controller 里加 @Valid 注解触发校验：
 * public Result<...> create(@Valid @RequestBody CreateDietRecordRequest request)
 */
@Data
public class CreateDietRecordRequest {

    /**
     * 记录日期
     * - 格式："2026-08-24"（ISO 8601 日期格式）
     * - 前端传字符串，Spring 自动转换为 LocalDate
     */
    @NotNull(message = "日期不能为空")
    private java.time.LocalDate recordDate;

    /**
     * 餐次
     * - 1：早餐
     * - 2：午餐
     * - 3：晚餐
     * - 4：加餐
     * - @Min(1) + @Max(4)：限制范围 1-4
     */
    @NotNull(message = "餐次不能为空")
    @Min(value = 1, message = "餐次范围 1-4")
    @Max(value = 4, message = "餐次范围 1-4")
    private Integer mealType;

    /**
     * 食物名称
     * - @NotBlank：不能为空或空字符串
     * - @Size(200)：对齐 diet_records.food_name（VARCHAR(200)），
     *   不先拦会在写库时被数据库截断报错，变成 500
     */
    @NotBlank(message = "食物名称不能为空")
    @Size(max = 200, message = "食物名称最长 200 个字符")
    private String foodName;

    /**
     * 热量（千卡）
     * - @Min(0)：不能为负数
     * - @Max：挡掉误输入的六位数量级，上限与前端 CALORIES_MAX 同值
     *
     * 关于小数（已知限制）：字段是 Integer，Jackson 会把 12.5 静默截断成 12 并返回 200，
     * 不报错。前端 record-edit 页靠 Number.isInteger 自己卡住，
     * 用其他客户端（Swagger / test-api.html）直接调用时请自行保证传整数。
     */
    @NotNull(message = "热量不能为空")
    @Min(value = 0, message = "热量不能为负数")
    @Max(value = 100000, message = "热量看起来过大（上限 100000）")
    private Integer calories;

    /**
     * 备注（可选）
     * - 长度上限对齐 diet_records.remark（VARCHAR(500)）
     * - 创建接口无“保持原值”语义，因此空串与不传一样都是「无备注」
     */
    @Size(max = 500, message = "备注最长 500 个字符")
    private String remark;
}
