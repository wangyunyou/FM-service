package com.wyy.fm.service.impl;

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

@Service
@RequiredArgsConstructor
public class DietRecordServiceImpl implements DietRecordService {

    private final DietRecordRepository dietRecordRepository;

    @Override
    @Transactional
    public DietRecordResponse create(Long userId, CreateDietRecordRequest request) {
        DietRecord record = new DietRecord();
        record.setUserId(userId);
        record.setRecordDate(request.getRecordDate());
        record.setMealType(request.getMealType());
        record.setFoodName(request.getFoodName());
        record.setCalories(request.getCalories());
        record.setRemark(request.getRemark());

        record = dietRecordRepository.save(record);
        return toResponse(record);
    }

    @Override
    @Transactional
    public DietRecordResponse update(Long userId, Long recordId, UpdateDietRecordRequest request) {
        DietRecord record = dietRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("记录不存在"));

        // 只能修改自己的记录
        if (!record.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权修改该记录");
        }

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

        record = dietRecordRepository.save(record);
        return toResponse(record);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long recordId) {
        DietRecord record = dietRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("记录不存在"));

        if (!record.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权删除该记录");
        }

        dietRecordRepository.delete(record);
    }

    @Override
    public DietStatisticsResponse queryWithStats(Long userId, QueryDietRecordRequest request) {
        // 校验日期范围
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期");
        }

        // 查询明细
        List<DietRecord> records = dietRecordRepository
                .findByUserIdAndRecordDateBetweenOrderByRecordDateAscMealTypeAsc(
                        userId, request.getStartDate(), request.getEndDate());

        // 统计总热量
        Integer totalCalories = dietRecordRepository
                .sumCaloriesByUserIdAndDateRange(userId, request.getStartDate(), request.getEndDate());

        // 按餐次统计
        List<Object[]> mealStats = dietRecordRepository
                .sumCaloriesByMealType(userId, request.getStartDate(), request.getEndDate());

        Map<String, Integer> caloriesByMeal = new HashMap<>();
        for (Object[] row : mealStats) {
            Integer mealType = (Integer) row[0];
            Integer calories = ((Number) row[1]).intValue();
            caloriesByMeal.put(DietRecordResponse.getMealTypeName(mealType), calories);
        }

        // 计算日均
        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        int avgCalories = records.isEmpty() ? 0 : totalCalories / (int) days;

        List<DietRecordResponse> recordResponses = records.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return DietStatisticsResponse.builder()
                .totalCalories(totalCalories)
                .caloriesByMeal(caloriesByMeal)
                .recordCount(records.size())
                .avgCaloriesPerDay(avgCalories)
                .records(recordResponses)
                .build();
    }

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
