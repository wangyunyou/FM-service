package com.wyy.fm;

import com.wyy.fm.common.JwtUtil;
import com.wyy.fm.dto.CreateDietRecordRequest;
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
 * - @ActiveProfiles("dev")：使用 dev 环境配置（H2 内存数据库）
 * - @Transactional：每个测试方法执行完自动回滚，不影响数据库
 * 
 * 测试内容：
 * - JWT 工具类测试
 * - 饮食记录查询测试
 * - HTTP 接口测试
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
     */
    @Test
    void testDietRecordQueriesWhenEmpty() {
        Long userId = 999L;  // 不存在的用户
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
     */
    @Test
    void testDietRecordQueriesWithData() {
        Long userId = 1L;
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
        User user = new User();
        user.setOpenid("test-openid-mock");
        user.setNickname("测试用户");
        user.setStatus(0);
        user = userRepository.save(user);

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
}
