package com.wyy.fm;

import com.wyy.fm.common.JwtUtil;
import com.wyy.fm.dto.DietStatisticsResponse;
import com.wyy.fm.dto.QueryDietRecordRequest;
import com.wyy.fm.model.DietRecord;
import com.wyy.fm.model.User;
import com.wyy.fm.repository.DietRecordRepository;
import com.wyy.fm.repository.UserRepository;
import com.wyy.fm.service.DietRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 集成测试类
 * 
 * 注解说明：
 * - @SpringBootTest：启动完整的 Spring 容器（真实环境）
 * - @AutoConfigureMockMvc：自动配置 MockMvc（模拟 HTTP 请求）
 * - @ActiveProfiles("dev")：使用 dev 配置，即本地 PostgreSQL（jdbc:postgresql://localhost:5432/fmdb）
 *   注意：跑测试前必须先启动本地 PG；data.sql 会先写入种子数据（测试用户 + 当天三餐）
 * - @Transactional：每个测试方法执行完自动回滚，测试自己造的数据不会落库
 *   （data.sql 的插入在容器启动时已提交，不受回滚影响）
 *
 * 测试内容：
 * - JWT 工具类测试
 * - 饮食记录统计测试（用独立用户，不依赖种子数据）
 * - HTTP 接口测试（含鉴权与参数校验回归）
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class FmApplicationTests {

    // 依赖注入（测试环境）
    @Autowired
    private MockMvc mockMvc;  // 模拟 HTTP 请求工具

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DietRecordRepository dietRecordRepository;

    @Autowired
    private DietRecordService dietRecordService;

    /**
     * 测试 Spring 容器是否正常启动
     */
    @Test
    void contextLoads() {
        assertNotNull(jwtUtil);  // 如果 jwtUtil 不为 null，说明容器启动成功
    }

    /**
     * 测试 JWT 工具类
     */
    @Test
    void testJwtTokenGenerationAndValidation() {
        Long userId = 12345L;
        
        // 1. 生成 token
        String token = jwtUtil.generateToken(userId);
        assertNotNull(token);  // token 不为空
        
        // 2. 验证 token
        assertTrue(jwtUtil.validateToken(token));  // token 有效
        
        // 3. 解析 token
        assertEquals(userId, jwtUtil.getUserIdFromToken(token));  // 解析出的 userId 正确
    }

    /**
     * 测试空数据查询
     *
     * 为什么现造一个用户而不是写死 userId：
     * - dev 库里有 data.sql 的种子数据，硬编码 ID 可能撞上真实记录导致断言飘
     */
    @Test
    void testDietRecordQueriesWhenEmpty() {
        Long userId = newTestUser("test-openid-empty").getId();
        LocalDate start = LocalDate.now().minusDays(7);
        LocalDate end = LocalDate.now();

        // 查询总热量（应该返回 0）
        Integer total = dietRecordRepository.sumCaloriesByUserIdAndDateRange(userId, start, end);
        assertNotNull(total);
        assertEquals(0, total);

        // 查询统计（应该返回空结果）
        QueryDietRecordRequest queryRequest = new QueryDietRecordRequest();
        queryRequest.setStartDate(start);
        queryRequest.setEndDate(end);
        DietStatisticsResponse stats = dietRecordService.queryWithStats(userId, queryRequest);
        assertNotNull(stats);
        assertEquals(0, stats.getTotalCalories());
        assertEquals(0, stats.getAvgCaloriesPerDay());
        assertEquals(0, stats.getRecordCount());
    }

    /**
     * 测试有数据时的查询
     *
     * 用独立用户隔离数据，避免与 data.sql 种子记录求和互干扰
     */
    @Test
    void testDietRecordQueriesWithData() {
        Long userId = newTestUser("test-openid-stats").getId();
        LocalDate today = LocalDate.now();

        // 1. 插入两条测试数据
        DietRecord r1 = new DietRecord();
        r1.setUserId(userId);
        r1.setRecordDate(today);
        r1.setMealType(1);  // 早餐
        r1.setFoodName("燕麦粥");
        r1.setCalories(300);
        dietRecordRepository.save(r1);

        DietRecord r2 = new DietRecord();
        r2.setUserId(userId);
        r2.setRecordDate(today);
        r2.setMealType(2);  // 午餐
        r2.setFoodName("牛肉饭");
        r2.setCalories(700);
        dietRecordRepository.save(r2);

        // 2. 查询总热量
        Integer total = dietRecordRepository.sumCaloriesByUserIdAndDateRange(userId, today, today);
        assertEquals(1000, total);  // 300 + 700 = 1000

        // 3. 查询统计
        QueryDietRecordRequest queryRequest = new QueryDietRecordRequest();
        queryRequest.setStartDate(today);
        queryRequest.setEndDate(today);
        DietStatisticsResponse stats = dietRecordService.queryWithStats(userId, queryRequest);
        assertEquals(1000, stats.getTotalCalories());
        assertEquals(1000, stats.getAvgCaloriesPerDay());
        assertEquals(2, stats.getRecordCount());
        assertEquals(300, stats.getCaloriesByMeal().get("早餐"));
        assertEquals(700, stats.getCaloriesByMeal().get("午餐"));
    }

    /**
     * 测试健康检查接口
     */
    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/health"))  // GET /health
                .andExpect(status().isOk())  // HTTP 200
                .andExpect(jsonPath("$.code").value(200))  // code = 200
                .andExpect(jsonPath("$.data.status").value("UP"));  // data.status = "UP"
    }

    /**
     * 测试版本号接口
     */
    @Test
    void testVersionEndpoint() throws Exception {
        mockMvc.perform(get("/version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.version").value("1.0.0"));
    }

    /**
     * 测试未授权请求（没有 token）
     */
    @Test
    void testUnauthorizedRequest() throws Exception {
        mockMvc.perform(get("/api/user/info"))  // 不带 token
                .andExpect(status().isUnauthorized())  // HTTP 401
                .andExpect(jsonPath("$.code").value(401));  // code = 401
    }

    /**
     * 测试完整的 API 流程
     */
    @Test
    void testAuthorizedApiFlow() throws Exception {
        // 1. 创建测试用户
        User user = newTestUser("test-openid-mock");

        // 2. 生成 token
        String token = jwtUtil.generateToken(user.getId());

        // 3. 测试获取用户信息
        mockMvc.perform(get("/api/user/info")
                        .header("Authorization", "Bearer " + token))  // 带上 token
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.nickname").value("测试用户"));

        // 4. 测试创建饮食记录
        String createJson = """
                {
                    "recordDate": "2026-08-24",
                    "mealType": 1,
                    "foodName": "鸡蛋全麦面包",
                    "calories": 350,
                    "remark": "早餐"
                }
                """;
        mockMvc.perform(post("/api/diet")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.foodName").value("鸡蛋全麦面包"))
                .andExpect(jsonPath("$.data.calories").value(350));

        // 5. 测试查询饮食记录
        mockMvc.perform(get("/api/diet/query")
                        .header("Authorization", "Bearer " + token)
                        .param("startDate", "2026-08-24")
                        .param("endDate", "2026-08-24"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalCalories").value(350))
                .andExpect(jsonPath("$.data.recordCount").value(1));
    }

    /**
     * 回归：PUT /api/diet/{id} 必须触发 DTO 参数校验
     *
     * 背景：Controller 入参漏写 @Valid 时，@Min/@Max 完全失效，
     * 热量能被改成负数、餐次能改成 9（库里出现枚举外的脏数据）
     */
    @Test
    void testUpdateDietRecordRejectsInvalidFields() throws Exception {
        User user = newTestUser("test-openid-update-valid");
        String token = jwtUtil.generateToken(user.getId());
        Long recordId = newTestRecord(user.getId(), 1, "鸡蛋", 70).getId();

        // 热量为负数 → 400
        mockMvc.perform(put("/api/diet/" + recordId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"calories\": -5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("热量不能为负数"));

        // 餐次越界 → 400
        mockMvc.perform(put("/api/diet/" + recordId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mealType\": 9}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        // 食物名称空串 → 400（部分更新语义下，置空请传 null）
        mockMvc.perform(put("/api/diet/" + recordId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"foodName\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        // 合法更新仍正常生效
        mockMvc.perform(put("/api/diet/" + recordId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"foodName\": \"燕麦片\", \"calories\": 150}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.foodName").value("燕麦片"))
                .andExpect(jsonPath("$.data.calories").value(150));
    }

    /**
     * 回归：查询参数缺失应返回 400，而不是被当作服务器异常给 500
     *
     * 背景：URL 查询参数绑定失败抛的是 BindException，
     * 它不是 MethodArgumentNotValidException 的子类（反而是父类），
     * 全局异常处理没接住就会落到兜底 Exception 分支
     */
    @Test
    void testQueryMissingParamsReturnsBadRequest() throws Exception {
        User user = newTestUser("test-openid-query-valid");
        String token = jwtUtil.generateToken(user.getId());

        // 两个日期都不传
        mockMvc.perform(get("/api/diet/query")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("日期不能为空")));

        // 只传开始日期
        mockMvc.perform(get("/api/diet/query")
                        .header("Authorization", "Bearer " + token)
                        .param("startDate", LocalDate.now().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * 测试用户数据隔离：只能改自己的记录
     */
    @Test
    void testUpdateOtherUsersRecordForbidden() throws Exception {
        User owner = newTestUser("test-openid-owner");
        User other = newTestUser("test-openid-other");
        Long recordId = newTestRecord(owner.getId(), 1, "牛奶", 100).getId();

        mockMvc.perform(put("/api/diet/" + recordId)
                        .header("Authorization", "Bearer " + jwtUtil.generateToken(other.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"calories\": 1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1003));  // NO_PERMISSION
    }

    /**
     * 造一个测试用户（openid 唯一，重复运行不会破唯一索引）
     */
    private User newTestUser(String openid) {
        User user = new User();
        user.setOpenid(openid);
        user.setNickname("测试用户");
        user.setStatus(0);
        return userRepository.save(user);
    }

    /**
     * 造一条测试饮食记录
     */
    private DietRecord newTestRecord(Long userId, Integer mealType, String foodName, Integer calories) {
        DietRecord record = new DietRecord();
        record.setUserId(userId);
        record.setRecordDate(LocalDate.now());
        record.setMealType(mealType);
        record.setFoodName(foodName);
        record.setCalories(calories);
        return dietRecordRepository.save(record);
    }
}
