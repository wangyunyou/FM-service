package com.wyy.fm.service.impl;

import com.wyy.fm.common.BusinessException;
import com.wyy.fm.common.ErrorCode;
import com.wyy.fm.dto.*;
import com.wyy.fm.model.DietRecord;
import com.wyy.fm.repository.DietRecordRepository;
import com.wyy.fm.service.DietRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 饮食记录服务实现类
 * 
 * 包含业务逻辑：
 * - 创建/更新/删除记录
 * - 权限校验（只能操作自己的记录）
 * - 统计计算（总热量、按餐次统计、日均等）
 */
@Service
@RequiredArgsConstructor
public class DietRecordServiceImpl implements DietRecordService {

    // 依赖注入
    private final DietRecordRepository dietRecordRepository;

    /**
     * 创建饮食记录
     * 
     * 流程：
     * 1. 将 DTO 转换为实体
     * 2. 保存到数据库
     * 3. 将实体转换为响应 DTO
     */
    @Override
    @Transactional
    public DietRecordResponse create(Long userId, CreateDietRecordRequest request) {
        // 1. 创建实体对象
        DietRecord record = new DietRecord();
        record.setUserId(userId);
        record.setRecordDate(request.getRecordDate());
        record.setMealType(request.getMealType());
        record.setFoodName(request.getFoodName());
        record.setCalories(request.getCalories());
        record.setRemark(request.getRemark());

        // 2. 保存（插入）到数据库
        record = dietRecordRepository.save(record);
        
        // 3. 转换为响应 DTO
        return toResponse(record);
    }

    /**
     * 更新饮食记录
     * 
     * 流程：
     * 1. 查询记录
     * 2. 权限校验（只能修改自己的）
     * 3. 部分更新（只更新传入的字段）
     * 4. 保存
     */
    @Override
    @Transactional
    public DietRecordResponse update(Long userId, Long recordId, UpdateDietRecordRequest request) {
        // 1. 查询记录
        DietRecord record = dietRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DIET_RECORD_NOT_FOUND));

        // 2. 权限校验：只能修改自己的记录
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        // 3. 部分更新（只更新传入的字段）
        if (request.getMealType() != null) {
            record.setMealType(request.getMealType());
        }
        if (request.getFoodName() != null) {
            record.setFoodName(request.getFoodName());
        }
        if (request.getCalories() != null) {
            record.setCalories(request.getCalories());
        }
        if (request.getRemark() != null) {
            record.setRemark(request.getRemark());
        }

        // 4. 保存
        record = dietRecordRepository.save(record);
        return toResponse(record);
    }

    /**
     * 删除饮食记录
     * 
     * 流程：
     * 1. 查询记录
     * 2. 权限校验
     * 3. 删除
     */
    @Override
    @Transactional
    public void delete(Long userId, Long recordId) {
        // 1. 查询记录
        DietRecord record = dietRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DIET_RECORD_NOT_FOUND));

        // 2. 权限校验：只能删除自己的记录
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        // 3. 删除
        dietRecordRepository.delete(record);
    }

    /**
     * 查询饮食记录 + 统计
     * 
     * 返回数据：
     * - 总热量
     * - 按餐次统计
     * - 记录条数
     * - 日均热量
     * - 明细列表
     */
    @Override
    public DietStatisticsResponse queryWithStats(Long userId, QueryDietRecordRequest request) {
        // 1. 校验日期范围
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new BusinessException(ErrorCode.DIET_DATE_INVALID);
        }

        // 2. 查询明细列表
        List<DietRecord> records = dietRecordRepository
                .findByUserIdAndRecordDateBetweenOrderByRecordDateAscMealTypeAsc(
                        userId, request.getStartDate(), request.getEndDate());

        // 3. 统计总热量
        Integer totalCalories = dietRecordRepository
                .sumCaloriesByUserIdAndDateRange(userId, request.getStartDate(), request.getEndDate());

        // 4. 按餐次统计
        List<Object[]> mealStats = dietRecordRepository
                .sumCaloriesByMealType(userId, request.getStartDate(), request.getEndDate());

        // 将查询结果转换为 Map<餐次名称, 热量>
        Map<String, Integer> caloriesByMeal = new HashMap<>();
        for (Object[] row : mealStats) {
            Integer mealType = (Integer) row[0];
            Integer calories = ((Number) row[1]).intValue();
            caloriesByMeal.put(DietRecordResponse.getMealTypeName(mealType), calories);
        }

        // 5. 计算日均热量
        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        int avgCalories = records.isEmpty() ? 0 : totalCalories / (int) days;

        // 6. 将实体列表转换为 DTO 列表
        // Stream API：函数式编程风格
        List<DietRecordResponse> recordResponses = records.stream()
                .map(this::toResponse)  // 将每个 DietRecord 转换为 DietRecordResponse
                .collect(Collectors.toList());

        // 7. 构建响应对象
        return DietStatisticsResponse.builder()
                .totalCalories(totalCalories)
                .caloriesByMeal(caloriesByMeal)
                .recordCount(records.size())
                .avgCaloriesPerDay(avgCalories)
                .records(recordResponses)
                .build();
    }

    /**
     * 实体 → DTO 转换方法
     * 
     * 作用：将数据库实体转换为前端需要的响应格式
     */
    private DietRecordResponse toResponse(DietRecord record) {
        return DietRecordResponse.builder()
                .id(record.getId())
                .recordDate(record.getRecordDate())
                .mealType(record.getMealType())
                .mealTypeName(DietRecordResponse.getMealTypeName(record.getMealType()))
                .foodName(record.getFoodName())
                .calories(record.getCalories())
                .remark(record.getRemark())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
