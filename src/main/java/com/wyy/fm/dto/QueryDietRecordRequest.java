package com.wyy.fm.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 饮食记录查询请求 DTO
 * 
 * 作用：前端调用 GET /api/diet/query 时传入的查询参数
 * 
 * 示例：
 * GET /api/diet/query?startDate=2026-08-01&endDate=2026-08-31
 * 
 * 注意：
 * - 这是 GET 请求，参数在 URL 里（不是 RequestBody）
 * - Spring 自动将 URL 参数映射到对象字段
 */
@Data
public class QueryDietRecordRequest {

    /**
     * 开始日期
     * - 格式："2026-08-01"
     */
    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    /**
     * 结束日期
     * - 格式："2026-08-31"
     * - 必须 >= startDate，否则 Service 层会报错
     */
    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;
}
