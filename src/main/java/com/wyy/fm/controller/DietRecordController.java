package com.wyy.fm.controller;

import com.wyy.fm.common.Result;
import com.wyy.fm.config.AuthInterceptor;
import com.wyy.fm.dto.*;
import com.wyy.fm.service.DietRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 饮食记录控制器
 * 
 * 接口列表：
 * - POST /api/diet：新增饮食记录
 * - PUT /api/diet/{id}：更新饮食记录
 * - DELETE /api/diet/{id}：删除饮食记录
 * - GET /api/diet/query：查询饮食记录 + 统计
 * 
 * 所有接口都需要 token（除了 /health）
 */
@RestController
@RequestMapping("/api/diet")
@RequiredArgsConstructor
@Tag(name = "饮食记录", description = "饮食记录的增删改查和统计")
public class DietRecordController {

    /**
     * 饮食记录服务（依赖注入）
     */
    private final DietRecordService dietRecordService;

    /**
     * 新增饮食记录接口
     * 
     * 请求：POST /api/diet
     * Header：Authorization: Bearer {token}
     * 请求体：{"recordDate":"2026-08-24", "mealType":1, "foodName":"鸡蛋", "calories":70}
     * 响应：{"code":200, "data":{"id":1, "recordDate":"2026-08-24", ...}}
     * 
     * 流程：
     * 1. 从 token 解析 userId（AuthInterceptor 做）
     * 2. 校验参数（@Valid 触发 DTO 里的校验规则）
     * 3. 调用 Service 创建记录
     * 4. 返回创建的记录详情
     */
    @PostMapping
    @Operation(summary = "创建饮食记录", description = "新增一条饮食记录，包含日期、餐次、食物名称和热量")
    public Result<DietRecordResponse> create(
            HttpServletRequest request,
            @Valid @RequestBody CreateDietRecordRequest createRequest) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.CURRENT_USER_ID);
        return Result.ok(dietRecordService.create(userId, createRequest));
    }

    /**
     * 更新饮食记录接口
     * 
     * 请求：PUT /api/diet/1
     * Header：Authorization: Bearer {token}
     * 请求体：{"foodName":"燕麦片", "calories":150}
     * 响应：{"code":200, "data":{"id":1, "foodName":"燕麦片", ...}}
     * 
     * 注解说明：
     * - @PathVariable Long id：从 URL 路径取参数（/api/diet/{id} 里的 {id}）
     * 
     * 权限校验：
     * - Service 层会检查 record.userId == currentUserId
     * - 只能修改自己的记录
     *
     * 部分更新语义：
     * - DTO 字段全部可选（无 @NotNull），null 表示"不改这个字段"
     * - 非 null 的值才参与校验和赋值，所以 @Min/@Max 只在字段被传时生效
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新饮食记录", description = "更新指定 ID 的饮食记录，只能更新自己的记录")
    public Result<DietRecordResponse> update(
            HttpServletRequest request,
            @Parameter(description = "饮食记录 ID", required = true)
            @PathVariable Long id,  // 从 URL 路径取记录 ID
            @Valid @RequestBody UpdateDietRecordRequest updateRequest) {  // @Valid 必须写：不写则 DTO 上的 @Min/@Max 全部失效
        Long userId = (Long) request.getAttribute(AuthInterceptor.CURRENT_USER_ID);
        return Result.ok(dietRecordService.update(userId, id, updateRequest));
    }

    /**
     * 删除饮食记录接口
     * 
     * 请求：DELETE /api/diet/1
     * Header：Authorization: Bearer {token}
     * 响应：{"code":200, "message":"success", "data":null}
     * 
     * 权限校验：
     * - Service 层会检查 record.userId == currentUserId
     * - 只能删除自己的记录
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除饮食记录", description = "删除指定 ID 的饮食记录，只能删除自己的记录")
    public Result<Void> delete(
            HttpServletRequest request,
            @Parameter(description = "饮食记录 ID", required = true)
            @PathVariable Long id) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.CURRENT_USER_ID);
        dietRecordService.delete(userId, id);
        return Result.ok();  // 删除成功，不返回数据
    }

    /**
     * 查询饮食记录 + 统计接口
     * 
     * 请求：GET /api/diet/query?startDate=2026-08-01&endDate=2026-08-31
     * Header：Authorization: Bearer {token}
     * 响应：{"code":200, "data":{"totalCalories":1800, "caloriesByMeal":{...}, "records":[...]}}
     * 
     * 参数说明：
     * - startDate、endDate：URL 查询参数（不是 RequestBody）
     * - Spring 自动将 URL 参数映射到 QueryDietRecordRequest 对象
     * 
     * 返回数据：
     * - totalCalories：总热量
     * - caloriesByMeal：按餐次统计
     * - recordCount：记录条数
     * - avgCaloriesPerDay：日均热量
     * - records：明细列表
     */
    @GetMapping("/query")
    @Operation(summary = "查询饮食记录", description = "按日期范围查询饮食记录，包含统计信息（总热量、日均、按餐次分组）")
    public Result<DietStatisticsResponse> query(
            HttpServletRequest request,
            @Valid QueryDietRecordRequest queryRequest) {  // URL 参数自动映射到对象
        Long userId = (Long) request.getAttribute(AuthInterceptor.CURRENT_USER_ID);
        return Result.ok(dietRecordService.queryWithStats(userId, queryRequest));
    }
}
