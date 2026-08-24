package com.wyy.fm.repository;

import com.wyy.fm.model.DietRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 饮食记录数据访问层
 * 
 * 作用：操作 diet_records 表（增删改查 + 统计）
 * 
 * 查询方法分类：
 * 1. 方法命名查询（Spring Data JPA 自动生成 SQL）
 * 2. @Query 自定义 JPQL（复杂查询手写 SQL）
 */
public interface DietRecordRepository extends JpaRepository<DietRecord, Long> {

    /**
     * 按用户 + 日期范围查询（按日期和餐次排序）
     * 
     * 生成的 SQL：
     * SELECT * FROM diet_records 
     * WHERE user_id = ? AND record_date BETWEEN ? AND ?
     * ORDER BY record_date ASC, meal_type ASC
     * 
     * 方法命名规则：
     * - findBy：查询
     * - UserId：user_id 字段
     * - And：并且
     * - RecordDateBetween：record_date 在范围内
     * - OrderBy：排序
     * - RecordDateAsc：按 record_date 升序
     * - MealTypeAsc：按 meal_type 升序
     * 
     * @param userId 用户 ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 符合条件的记录列表
     */
    List<DietRecord> findByUserIdAndRecordDateBetweenOrderByRecordDateAscMealTypeAsc(
            Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 按用户 + 某天查询（按餐次排序）
     * 
     * 生成的 SQL：
     * SELECT * FROM diet_records 
     * WHERE user_id = ? AND record_date = ?
     * ORDER BY meal_type ASC
     * 
     * @param userId 用户 ID
     * @param date 指定日期
     * @return 该用户当天的所有记录
     */
    List<DietRecord> findByUserIdAndRecordDateOrderByMealTypeAsc(Long userId, LocalDate date);

    /**
     * 统计某用户某日期范围的总热量
     * 
     * 生成的 SQL：
     * SELECT COALESCE(SUM(calories), 0) FROM diet_records
     * WHERE user_id = ? AND record_date BETWEEN ? AND ?
     * 
     * 注解说明：
     * - @Query：手写 JPQL（Java Persistence Query Language，类似 SQL 但操作的是实体）
     * - COALESCE：SQL 函数，如果 SUM 结果为 null，返回 0
     * - :userId：命名参数，用 @Param 绑定
     * 
     * @param userId 用户 ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 总热量（千卡）
     */
    @Query("SELECT COALESCE(SUM(d.calories), 0) FROM DietRecord d " +
            "WHERE d.userId = :userId AND d.recordDate BETWEEN :startDate AND :endDate")
    Integer sumCaloriesByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 按餐次统计热量
     * 
     * 生成的 SQL：
     * SELECT meal_type, SUM(calories) FROM diet_records
     * WHERE user_id = ? AND record_date BETWEEN ? AND ?
     * GROUP BY meal_type
     * 
     * 返回结果示例：
     * [[1, 400], [2, 700], [3, 600]]  // [餐次, 热量]
     * 
     * @return List<Object[]>：每行是一个数组 [mealType, totalCalories]
     */
    @Query("SELECT d.mealType, SUM(d.calories) FROM DietRecord d " +
            "WHERE d.userId = :userId AND d.recordDate BETWEEN :startDate AND :endDate " +
            "GROUP BY d.mealType")
    List<Object[]> sumCaloriesByMealType(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
