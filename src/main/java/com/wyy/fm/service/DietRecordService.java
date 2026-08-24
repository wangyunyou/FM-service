package com.wyy.fm.service;

import com.wyy.fm.dto.*;

public interface DietRecordService {

    /** 新增饮食记录 */
    DietRecordResponse create(Long userId, CreateDietRecordRequest request);

    /** 更新饮食记录 */
    DietRecordResponse update(Long userId, Long recordId, UpdateDietRecordRequest request);

    /** 删除饮食记录 */
    void delete(Long userId, Long recordId);

    /** 查询日期范围内的饮食记录 + 统计 */
    DietStatisticsResponse queryWithStats(Long userId, QueryDietRecordRequest request);
}
