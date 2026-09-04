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

import java.nio.charset.StandardCharsets;
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
 *   注意：跑测试前必须先启动本地 PG
 * - @Transactional：每个测试方法执行完自动回滚，测试自己造的数据不会落库
 *
 * 测试内容：
 * - JWT 工具类测试
 * - 饮食记录统计测试（用独立用户隔离数据）
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
     * - 硬编码 ID 可能撞上真实记录导致断言飘，通过独立 openid 隔绝环境影响
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
     * 用独立用户隔离数据，避免与其他测试记录求和互干扰
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
     * 回归测试：日均热量的分母必须是「有记录的天数」
     *
     * 背景：旧实现用「查询区间总天数」做分母，导致查整月只记了 1 天时，
     *      300 kcal 被算成 300/31 = 9，前端展示出一个毫无意义的日均值
     * 期望：300 kcal / 1 个记录日 = 300
     *
     * 类上有 @Transactional，本方法造的数据会自动回滚，不会污染 dev 库
     */
    @Test
    void testAvgCaloriesPerDayUsesRecordedDays() {
        Long userId = newTestUser("test-openid-avg").getId();
        LocalDate today = LocalDate.now();

        // 只在今天记 1 条
        DietRecord record = new DietRecord();
        record.setUserId(userId);
        record.setRecordDate(today);
        record.setMealType(1);  // 早餐
        record.setFoodName("苹果");
        record.setCalories(300);
        dietRecordRepository.save(record);

        // 但查询区间横跨 31 天
        QueryDietRecordRequest queryRequest = new QueryDietRecordRequest();
        queryRequest.setStartDate(today.minusDays(30));
        queryRequest.setEndDate(today);

        DietStatisticsResponse stats = dietRecordService.queryWithStats(userId, queryRequest);

        assertEquals(300, stats.getTotalCalories());
        assertEquals(1, stats.getRecordCount());
        assertEquals(300, stats.getAvgCaloriesPerDay());  // 关键断言：旧代码这里会返回 9
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

    // ==================== 2026-08-28 代码审查后补的回归测试 ====================
    // 下面每一组都对应一条实测复现过的问题，改坏了会立刻红。

    /**
     * 回归：禁用账号（status=1）既不能登录，也不能继续读个人数据
     *
     * 背景：users.status 一直有值、ErrorCode.USER_DISABLED 与前端重登码集也都定义了，
     *      但后端从没读过这个字段 —— 实测把 status 改成 1 后，
     *      带旧 token 查询返回 200、再登录还能拿到新 token，"禁用"完全无效。
     */
    @Test
    void disabledUserCannotLoginNorReadProfile() throws Exception {
        // 用 mock 登录建账号（dev profile 下任意 code 可换 token），
        // 这样后面的“重新登录”走的确实是同一个 openid，而不是又新建一个用户
        String token = loginByCode("case-disabled");

        // 禁用前先确认可用
        mockMvc.perform(get("/api/user/info").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Long userId = jwtUtil.getUserIdFromToken(token);
        userRepository.findById(userId).ifPresent(u -> {
            u.setStatus(1);
            userRepository.saveAndFlush(u);
        });

        // 旧 token 继续用 → 1002（前端 request 层据此清 token 跳登录页）
        mockMvc.perform(get("/api/user/info").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1002));

        // 重新登录 → 同样 1002，不再发新 token
        mockMvc.perform(post("/api/user/wx-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"case-disabled\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1002));
    }

    /**
     * 回归：老用户重新登录不得被刷掉自己改过的昵称/性别
     *
     * 背景：微信现在的 getUserProfile 只能拿到固定默认值（"微信用户" + 灰头像），
     *      而 wxLogin 原来只判 null 就覆盖 —— 实测用户自己改名后再登录一次，
     *      昵称被打回"微信用户"、性别被打回 0。
     * 口径：初始资料只写入「服务端本次真的新建了账号」的用户；
     *      客户端上报的 isNewUser 只用于对账，不参与决策（token 被清后老用户重登也会自报首登）。
     */
    @Test
    void returningUserProfileIsNotOverwritten() throws Exception {
        // 首登：服务端建号，昵称按前端带上来的初始值写入
        mockMvc.perform(post("/api/user/wx-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"case-no-overwrite\",\"nickname\":\"微信用户\",\"gender\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("微信用户"))
                .andExpect(jsonPath("$.data.isNewUser").value(true));
        String token = loginByCode("case-no-overwrite");

        // 用户自己在「我的」页改过资料
        mockMvc.perform(put("/api/user/info")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"我自己起的名字\",\"gender\":2}"))
                .andExpect(status().isOk());

        // 老用户重登：前端仍会把微信默认昵称带上来（无论它自报 isNewUser 是 true 还是 false），
        // 服务端都必须因为"账号已存在"而忽略这三项
        mockMvc.perform(post("/api/user/wx-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"case-no-overwrite\",\"nickname\":\"微信用户\",\"gender\":0,\"isNewUser\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isNewUser").value(false))
                .andExpect(jsonPath("$.data.nickname").value("我自己起的名字"));

        mockMvc.perform(get("/api/user/info").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("我自己起的名字"))
                .andExpect(jsonPath("$.data.gender").value(2));

        // 自报 false 也一样（走的是同一条服务端判据）
        mockMvc.perform(post("/api/user/wx-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"case-no-overwrite\",\"nickname\":\"又被刷\",\"isNewUser\":false}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/user/info").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.nickname").value("我自己起的名字"));
    }

    /**
     * 回归：remark 传空串必须能清空备注（省略键与传 null 都代表"不改"）
     *
     * 背景：前端 JSON.stringify 会丢掉值为 undefined 的键，
     *      而库里已有的备注既清不掉、也发不出"清空"意图 —— 实测三种发法都改不动原值。
     */
    @Test
    void emptyRemarkClearsItButOmittedKeepsIt() throws Exception {
        User user = newTestUser("test-openid-remark");
        String token = jwtUtil.generateToken(user.getId());
        Long recordId = newTestRecord(user.getId(), 1, "鸡蛋", 70).getId();

        // 先写上一条备注
        mockMvc.perform(put("/api/diet/" + recordId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remark\":\"加了糖\"}"))
                .andExpect(jsonPath("$.data.remark").value("加了糖"));

        // 省略 remark 键（前端"没清空"时的实际发法）→ 保持原值
        mockMvc.perform(put("/api/diet/" + recordId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"calories\":80}"))
                .andExpect(jsonPath("$.data.remark").value("加了糖"));

        // 显式 null → 仍是"不改"
        mockMvc.perform(put("/api/diet/" + recordId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remark\":null}"))
                .andExpect(jsonPath("$.data.remark").value("加了糖"));

        // 空串 → 清空（响应里 default-property-inclusion=non_null，字段会直接消失）
        mockMvc.perform(put("/api/diet/" + recordId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remark\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.remark").doesNotExist());
    }

    /**
     * 回归：字符串"必填"字段不得被纯空白糊过去
     *
     * 背景：foodName 原来写的是 @Size(min=1)，实测 {"foodName":"   "} 直接 200 入库，
     *      列表里出现一条"看不见的名字"。现已统一为 @NotBlank + 写入前 trim。
     */
    @Test
    void blankStringsAreRejected() throws Exception {
        User user = newTestUser("test-openid-blank");
        String token = jwtUtil.generateToken(user.getId());
        Long recordId = newTestRecord(user.getId(), 1, "牛奶", 100).getId();

        mockMvc.perform(put("/api/diet/" + recordId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"foodName\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(put("/api/user/info")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("昵称不能为空"));
    }

    /**
     * 回归：热量上限与日期不得在未来（前端挡了，后端也必须挡）
     *
     * 背景：CALORIES_MAX 与"日期不能是未来"只写在小程序里，
     *      实测用 curl 能记 2099-01-01、1900-01-01，以及 200000 kcal 的记录并返回 200。
     */
    @Test
    void caloriesUpperBoundAndFutureDateAreRejected() throws Exception {
        User user = newTestUser("test-openid-bounds");
        String token = jwtUtil.generateToken(user.getId());

        mockMvc.perform(post("/api/diet")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recordDate\":\"" + LocalDate.now() + "\",\"mealType\":1,\"foodName\":\"巨餐\",\"calories\":100001}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(post("/api/diet")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recordDate\":\"2099-01-01\",\"mealType\":1,\"foodName\":\"未来的饭\",\"calories\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2002))
                .andExpect(jsonPath("$.message").value("记录日期不能晚于今天"));

        // 查询的 endDate 在未来不再报错，见下面 queryClampsFutureEndDateToToday
        mockMvc.perform(get("/api/diet/query")
                        .header("Authorization", "Bearer " + token)
                        .param("startDate", "2020-01-01")
                        .param("endDate", "2099-12-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2003));
    }

    /**
     * 回归：查询的 endDate 在未来必须「收敛到今天」，而不是报错
     *
     * 背景（上一轮我自己引入的故障）：给 endDate 加了「不得晚于今天」的硬校验后，
     *      首页 currentWeekRange() 返回的「周一~周日」在周五查就是查到未来，
     *      后端返回 2002 → 前端 Promise.all 整个 reject → 今日记录被清空，
     *      还弹一条「结束日期不能晚于今天」（用户在模拟器里直接看到）。
     *      统计页的「本周」「本月」两个预设 tab 同理。
     * 期望：未来日期收敛到今天（数据完全等价，未来不可能有记录），
     *      只有整个区间都在未来（startDate 也晚于今天）才算区间不合法。
     */
    @Test
    void queryClampsFutureEndDateToToday() throws Exception {
        User user = newTestUser("test-openid-clamp");
        String token = jwtUtil.generateToken(user.getId());
        LocalDate today = LocalDate.now();
        newTestRecord(user.getId(), 1, "鸡蛋", 70);

        // 1) 周五查「本周」这种末端落在未来的区间：200，且今天的记录要查得到
        //    （今天往前 4 天 = 本周一，今天往后 2 天 = 本周日，与前端 currentWeekRange 同形）
        mockMvc.perform(get("/api/diet/query")
                        .header("Authorization", "Bearer " + token)
                        .param("startDate", today.minusDays(4).toString())
                        .param("endDate", today.plusDays(2).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalCalories").value(70))
                .andExpect(jsonPath("$.data.recordCount").value(1));

        // 2) 整个区间都在未来 → 区间不合法（这条是真错误，不是末端越界）
        mockMvc.perform(get("/api/diet/query")
                        .header("Authorization", "Bearer " + token)
                        .param("startDate", today.plusDays(1).toString())
                        .param("endDate", today.plusDays(5).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2002))
                .andExpect(jsonPath("$.message").value("开始日期不能晚于今天"));

        // 3) 跨度按收敛后的区间算：startDate=今天、endDate=2099 收敛成一天，不该报 2003
        mockMvc.perform(get("/api/diet/query")
                        .header("Authorization", "Bearer " + token)
                        .param("startDate", today.toString())
                        .param("endDate", "2099-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.recordCount").value(1));
    }

    /**
     * 回归：查询跨度必须有上限（接口无分页，跨度 = 一次返回的行数）
     *
     * 背景：GET /api/diet/query 一次吐区间内全部记录，而自定义区间的开始日期
     *      原本可以拖到 2020 年 —— 不限制跨度就等于允许一条请求拉出某账号全部历史。
     *      现已限制 366 天（2003），前端 Picker 也按结束日期倒推同样的下界。
     */
    @Test
    void queryRangeSpanIsCapped() throws Exception {
        User user = newTestUser("test-openid-span");
        String token = jwtUtil.generateToken(user.getId());
        LocalDate today = LocalDate.now();

        // 366 天：边界内，放行
        mockMvc.perform(get("/api/diet/query")
                        .header("Authorization", "Bearer " + token)
                        .param("startDate", today.minusDays(365).toString())
                        .param("endDate", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 367 天：越界，2003
        mockMvc.perform(get("/api/diet/query")
                        .header("Authorization", "Bearer " + token)
                        .param("startDate", today.minusDays(366).toString())
                        .param("endDate", today.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2003));
    }

    /**
     * 回归：路径参数不是数字要返回 400 + 可读文案，而不是 500「服务器内部错误」
     *
     * 背景：MethodArgumentTypeMismatchException 此前没被接住，实测 PUT /api/diet/abc 返回 500；
     *      而日期参数转换失败虽然进了 BindException 分支，
     *      却把 "Failed to convert property value of type 'java.lang.String' ..." 整段甩给用户
     *      （前端 request 层会原样 toast）。
     */
    @Test
    void typeMismatchReturnsReadableBadRequest() throws Exception {
        User user = newTestUser("test-openid-typemismatch");
        String token = jwtUtil.generateToken(user.getId());

        mockMvc.perform(put("/api/diet/abc")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"calories\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                // 文案必须是「字段中文名 + 格式不正确」：以前这里是 500「服务器内部错误」
                .andExpect(jsonPath("$.message").value("记录 ID格式不正确"));

        // 必须显式按 UTF-8 取响应体：getContentAsString() 不传参时按 ISO-8859-1 解码，
        // 中文提示会读成乱码，导致"包含中文"的断言假失败
        String message = mockMvc.perform(get("/api/diet/query")
                        .header("Authorization", "Bearer " + token)
                        .param("startDate", "2026-02-30")
                        .param("endDate", LocalDate.now().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertFalse(message.contains("java.lang.String"), "不得把 Spring 内部类型文案透给用户");
        assertFalse(message.contains("jakarta.validation"), "不得把注解全限定名透给用户");
        assertTrue(message.contains("开始日期"), "应给出中文字段名提示");
    }

    /**
     * 回归：按餐次统计时，若有多个不同未知餐次代码映射为「未知」，热量必须累加而不是相互覆盖
     */
    @Test
    void testUnknownMealTypesAreSummedNotOverwritten() {
        Long userId = newTestUser("test-openid-meal-merge").getId();
        LocalDate today = LocalDate.now();

        // 插入两条异常餐次代码的数据（直接落库绕过 DTO 校验）
        DietRecord r1 = new DietRecord();
        r1.setUserId(userId);
        r1.setRecordDate(today);
        r1.setMealType(0); // 未知餐次 0
        r1.setFoodName("食物A");
        r1.setCalories(100);
        dietRecordRepository.save(r1);

        DietRecord r2 = new DietRecord();
        r2.setUserId(userId);
        r2.setRecordDate(today);
        r2.setMealType(99); // 未知餐次 99
        r2.setFoodName("食物B");
        r2.setCalories(200);
        dietRecordRepository.save(r2);

        QueryDietRecordRequest queryRequest = new QueryDietRecordRequest();
        queryRequest.setStartDate(today);
        queryRequest.setEndDate(today);

        DietStatisticsResponse stats = dietRecordService.queryWithStats(userId, queryRequest);
        assertEquals(300, stats.getTotalCalories());
        assertEquals(300, stats.getCaloriesByMeal().get("未知"), "多个映射为「未知」的餐次热量应累加为 300，而不是被覆盖");
    }

    /**
     * 走 mock 登录拿 token（dev profile 下 wx.miniapp.mock-enabled=true，任意 code 可换）
     *
     * 为什么需要它：mock 的 openid 是 code 的 SHA-256 前 32 位，Java 侧算不出来，
     * 手工 new 一个 User 再登录就变成两个不同账号 —— 涉及登录行为的回归只能用这个方法建号。
     */
    private String loginByCode(String code) throws Exception {
        String body = mockMvc.perform(post("/api/user/wx-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String token = body.replaceAll(".*\"token\":\"", "").replaceAll("\".*", "");
        assertFalse(token.isEmpty(), "mock 登录必须返回 token");
        return token;
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
