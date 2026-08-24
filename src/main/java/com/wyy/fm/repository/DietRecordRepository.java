package com.wyy.fm.repository;

import com.wyy.fm.model.DietRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DietRecordRepository extends JpaRepository<DietRecord, Long> {

    /** 按用户+日期范围查询 */
    List<DietRecord> findByUserIdAndRecordDateBetweenOrderByRecordDateAscMealTypeAsc(
            Long userId, LocalDate startDate, LocalDate endDate);

    /** 按用户+某天查询 */
    List<DietRecord> findByUserIdAndRecordDateOrderByMealTypeAsc(Long userId, LocalDate date);

    /** 统计某用户某日期范围的总热量 */
    @Query("SELECT COALESCE(SUM(d.calories), 0) FROM DietRecord d " +
            "WHERE d.userId = :userId AND d.recordDate BETWEEN :startDate AND :endDate")
    Integer sumCaloriesByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /** 按餐次统计热量 */
    @Query("SELECT d.mealType, SUM(d.calories) FROM DietRecord d " +
            "WHERE d.userId = :userId AND d.recordDate BETWEEN :startDate AND :endDate " +
            "GROUP BY d.mealType")
    List<Object[]> sumCaloriesByMealType(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
