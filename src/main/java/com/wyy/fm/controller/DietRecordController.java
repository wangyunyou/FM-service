package com.wyy.fm.controller;

import com.wyy.fm.common.Result;
import com.wyy.fm.config.AuthInterceptor;
import com.wyy.fm.dto.*;
import com.wyy.fm.service.DietRecordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/diet")
@RequiredArgsConstructor
public class DietRecordController {

    private final DietRecordService dietRecordService;

    /**
     * 新增饮食记录
     */
    @PostMapping
    public Result<DietRecordResponse> create(
            HttpServletRequest request,
            @Valid @RequestBody CreateDietRecordRequest createRequest) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.CURRENT_USER_ID);
        return Result.ok(dietRecordService.create(userId, createRequest));
    }

    /**
     * 更新饮食记录
     */
    @PutMapping("/{id}")
    public Result<DietRecordResponse> update(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestBody UpdateDietRecordRequest updateRequest) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.CURRENT_USER_ID);
        return Result.ok(dietRecordService.update(userId, id, updateRequest));
    }

    /**
     * 删除饮食记录
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            HttpServletRequest request,
            @PathVariable Long id) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.CURRENT_USER_ID);
        dietRecordService.delete(userId, id);
        return Result.ok();
    }

    /**
     * 查询饮食记录 + 统计（日期范围）
     */
    @GetMapping("/query")
    public Result<DietStatisticsResponse> query(
            HttpServletRequest request,
            @Valid QueryDietRecordRequest queryRequest) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.CURRENT_USER_ID);
        return Result.ok(dietRecordService.queryWithStats(userId, queryRequest));
    }
}
