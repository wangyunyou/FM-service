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

import java.time.LocalDate;
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

    /**
     * 单次查询允许的最大跨度（天，含首尾）
     *
     * 为什么必须有：GET /api/diet/query 没有分页，一次返回区间内**全部**记录，
     * 而前端自定义区间可以把开始日期拖到 2020 年 —— 不限制跨度就等于
     * 允许任何人用一条请求把某个账号的全部历史（以及整张表的热度）拉出来。
     * 366 是"最多查一年"的整数上界：预设区间最长是本月（31 天）、近 7 天，
     * 自定义场景留足余量，同时把 29000 天这种病态区间挡掉。
     * 真要放开，得先给接口加分页并同步前端。
     */
    private static final long MAX_QUERY_RANGE_DAYS = 366;

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
        // 0. 日期合理性：只往后记流水，不允许预支未来的饭
        //    前端 Picker 已把 end 锁在今天，但其他客户端（Swagger / test-api.html）能递任意日期，
        //    不拦住就会写进永远对不上的脏数据（实测 2099-01-01 / 1900-01-01 曾直接 200 入库）
        requireNotFuture(request.getRecordDate(), "记录日期");

        // 1. 创建实体对象（字符串字段统一 trim，不把尾随空格写进库）
        DietRecord record = new DietRecord();
        record.setUserId(userId);
        record.setRecordDate(request.getRecordDate());
        record.setMealType(request.getMealType());
        record.setFoodName(request.getFoodName().trim());
        record.setCalories(request.getCalories());
        record.setRemark(trimToNull(request.getRemark()));

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
        //    字符串字段口径：null = 不改；非 null = 用 trim 后的新值覆盖；
        //    备注比较特殊，“空串”就是合法的清空意图（前端清空备注按钮依赖这一条）
        if (request.getMealType() != null) {
            record.setMealType(request.getMealType());
        }
        if (request.getFoodName() != null) {
            record.setFoodName(request.getFoodName().trim());
        }
        if (request.getCalories() != null) {
            record.setCalories(request.getCalories());
        }
        if (request.getRemark() != null) {
            // 传了非 null 就是“要改”；改成空串/纯空白则归一存 NULL，库里不留“空字符串备注”
            record.setRemark(trimToNull(request.getRemark()));
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
        //    结束日期不允许在未来：前端 Picker 锁了 end，但其他客户端能递 2099-12-31，
        //    那会把“区间天数”算成一个无意义的巨值，统计口径直接失真
        requireNotFuture(request.getEndDate(), "结束日期");
        //    跨度上限：接口无分页，跨度就是一次的返回量（见 MAX_QUERY_RANGE_DAYS 注释）
        long span = request.getEndDate().toEpochDay() - request.getStartDate().toEpochDay() + 1;
        if (span > MAX_QUERY_RANGE_DAYS) {
            throw new BusinessException(ErrorCode.DIET_DATE_RANGE_TOO_LONG);
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
        //    分母用「有记录的天数」，不是查询区间的总天数
        //    否则查整月（31 天）但只记了 1 天时，会算出 900/31=29 这种无意义的数字
        //    records 上一步已经查出来了，这里用 Stream 去重计数即可，不必再访问数据库
        long days = records.stream()
                .map(DietRecord::getRecordDate)   // 取每条记录的日期
                .distinct()                       // 同一天多条只算一次
                .count();

        // days 为 0 说明区间内没有任何记录，直接返回 0（同时避免了除零异常）
        int avgCalories = days == 0 ? 0 : totalCalories / (int) days;

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

    /**
     * 拒绝「今天之后」的日期
     *
     * 为什么用 LocalDate.now() 而不是 UTC：本项目的日期全程是“本地自然日”口径
     * （前端 date.ts 也一律按本地零点构造），两边保持一致才能避免跨零点差一天。
     */
    private void requireNotFuture(LocalDate date, String label) {
        if (date != null && date.isAfter(LocalDate.now())) {
            throw new BusinessException(ErrorCode.DIET_DATE_INVALID, label + "不能晚于今天");
        }
    }

    /**
     * 可选字符串字段：trim 后为空则归一为 null
     * - 创建：不传 / 空串都是「无备注」，统一存 NULL
     * - 更新：null 代表「不改」（在上面分支已拦），能走到这里的空串就是「清空」意图
     */
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
