package com.wyy.fm.service;

import com.wyy.fm.dto.*;

/**
 * 饮食记录服务接口
 * 
 * 作用：定义饮食记录相关的业务逻辑
 * 
 * 实现类：DietRecordServiceImpl
 */
public interface DietRecordService {

    /**
     * 新增饮食记录
     * 
     * @param userId 当前用户 ID
     * @param request 创建请求（日期、餐次、食物名称、热量等）
     * @return 创建的记录详情
     */
    DietRecordResponse create(Long userId, CreateDietRecordRequest request);

    /**
     * 更新饮食记录
     * 
     * @param userId 当前用户 ID
     * @param recordId 要更新的记录 ID
     * @param request 更新请求（部分更新）
     * @return 更新后的记录详情
     * @throws com.wyy.fm.common.BusinessException 如果记录不存在或无权操作
     */
    DietRecordResponse update(Long userId, Long recordId, UpdateDietRecordRequest request);

    /**
     * 删除饮食记录
     * 
     * @param userId 当前用户 ID
     * @param recordId 要删除的记录 ID
     * @throws com.wyy.fm.common.BusinessException 如果记录不存在或无权操作
     */
    void delete(Long userId, Long recordId);

    /**
     * 查询日期范围内的饮食记录 + 统计
     * 
     * @param userId 当前用户 ID
     * @param request 查询请求（开始日期、结束日期）
     * @return 统计结果（总热量、按餐次统计、明细列表等）
     */
    DietStatisticsResponse queryWithStats(Long userId, QueryDietRecordRequest request);
}
