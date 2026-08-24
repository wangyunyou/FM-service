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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class FmApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DietRecordRepository dietRecordRepository;

    @Autowired
    private DietRecordService dietRecordService;

    @Test
    void contextLoads() {
        assertNotNull(jwtUtil);
    }

    @Test
    void testJwtTokenGenerationAndValidation() {
        Long userId = 12345L;
        String token = jwtUtil.generateToken(userId);
        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));
        assertEquals(userId, jwtUtil.getUserIdFromToken(token));
    }

    @Test
    void testDietRecordQueriesWhenEmpty() {
        Long userId = 999L;
        LocalDate start = LocalDate.now().minusDays(7);
        LocalDate end = LocalDate.now();

        Integer total = dietRecordRepository.sumCaloriesByUserIdAndDateRange(userId, start, end);
        assertNotNull(total);
        assertEquals(0, total);

        QueryDietRecordRequest queryRequest = new QueryDietRecordRequest();
        queryRequest.setStartDate(start);
        queryRequest.setEndDate(end);
        DietStatisticsResponse stats = dietRecordService.queryWithStats(userId, queryRequest);
        assertNotNull(stats);
        assertEquals(0, stats.getTotalCalories());
        assertEquals(0, stats.getAvgCaloriesPerDay());
        assertEquals(0, stats.getRecordCount());
    }

    @Test
    void testDietRecordQueriesWithData() {
        Long userId = 1L;
        LocalDate today = LocalDate.now();

        DietRecord r1 = new DietRecord();
        r1.setUserId(userId);
        r1.setRecordDate(today);
        r1.setMealType(1);
        r1.setFoodName("燕麦粥");
        r1.setCalories(300);
        dietRecordRepository.save(r1);

        DietRecord r2 = new DietRecord();
        r2.setUserId(userId);
        r2.setRecordDate(today);
        r2.setMealType(2);
        r2.setFoodName("牛肉饭");
        r2.setCalories(700);
        dietRecordRepository.save(r2);

        Integer total = dietRecordRepository.sumCaloriesByUserIdAndDateRange(userId, today, today);
        assertEquals(1000, total);

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

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void testUnauthorizedRequest() throws Exception {
        mockMvc.perform(get("/api/user/info"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void testAuthorizedApiFlow() throws Exception {
        User user = new User();
        user.setOpenid("test-openid-mock");
        user.setNickname("测试用户");
        user.setStatus(0);
        user = userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId());

        // 1. Get User Info
        mockMvc.perform(get("/api/user/info")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.nickname").value("测试用户"));

        // 2. Create Diet Record
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

        // 3. Query Diet Record
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
