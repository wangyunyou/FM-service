package com.wyy.fm.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * StartupSafetyCheck 单元测试
 *
 * 为什么不启 Spring 容器：
 * - 被测逻辑只依赖 Environment + 两个配置值，用 MockEnvironment 直接构造即可，
 *   比 @SpringBootTest 快几个数量级，也不会碰到真实数据库
 * - 配置值本身是 @Value 注入的私有字段，测试里用 ReflectionTestUtils 赋值
 *
 * 覆盖的安全边界：
 * - dev profile：开发便利开关允许
 * - 非 dev：mock 登录、含通配符或 "null" 的跨域来源、弱/示例 JWT 密钥一律拒绝启动
 * - 非 dev：具体域名 / 留空 + 合格密钥放行
 */
class StartupSafetyCheckTests {

    /**
     * 一个足够长的合规格测试密钥（仅测试用，不是任何真实凭证）
     */
    private static final String VALID_SECRET = "unit-test-secret-value-0123456789-abcdefghij";

    /**
     * 构造被测对象（使用合规格 JWT 密钥）
     *
     * @param devProfile        是否激活 dev profile
     * @param mockLoginEnabled   微信登录 mock 开关
     * @param corsOrigins       跨域来源配置（null 表示未配置）
     */
    private StartupSafetyCheck newCheck(boolean devProfile, boolean mockLoginEnabled, List<String> corsOrigins) {
        return newCheck(devProfile, mockLoginEnabled, corsOrigins, VALID_SECRET);
    }

    /**
     * 构造被测对象（完整入参版）
     *
     * @param jwtSecret 待测的 JWT 密钥
     */
    private StartupSafetyCheck newCheck(boolean devProfile, boolean mockLoginEnabled,
                                       List<String> corsOrigins, String jwtSecret) {
        return newCheck(devProfile, mockLoginEnabled, corsOrigins, jwtSecret, false);
    }

    /**
     * 构造被测对象（含接口文档开关）
     *
     * @param apiDocsEnabled springdoc.api-docs.enabled 的值
     */
    private StartupSafetyCheck newCheck(boolean devProfile, boolean mockLoginEnabled, List<String> corsOrigins,
                                       String jwtSecret, boolean apiDocsEnabled) {
        MockEnvironment environment = new MockEnvironment();
        if (devProfile) {
            environment.setActiveProfiles(StartupSafetyCheck.DEV_PROFILE);
        }
        StartupSafetyCheck check = new StartupSafetyCheck(environment);
        ReflectionTestUtils.setField(check, "mockLoginEnabled", mockLoginEnabled);
        ReflectionTestUtils.setField(check, "corsAllowedOriginPatterns", corsOrigins);
        ReflectionTestUtils.setField(check, "jwtSecret", jwtSecret);
        ReflectionTestUtils.setField(check, "apiDocsEnabled", apiDocsEnabled);
        return check;
    }

    @Test
    void devProfileAllowsDevOnlySwitches() {
        StartupSafetyCheck check = newCheck(true, true, List.of("*", "null"));
        assertDoesNotThrow(check::verifyProductionSafety);
    }

    @Test
    void productionRejectsMockLogin() {
        StartupSafetyCheck check = newCheck(false, true, List.of());
        IllegalStateException ex = assertThrows(IllegalStateException.class, check::verifyProductionSafety);
        // 报错信息必须能自解释：开关名 + 后果 + 怎么改
        assertTrue(ex.getMessage().contains("wx.miniapp.mock-enabled"), "错误信息应指出具体配置项");
        assertTrue(ex.getMessage().contains("profiles=dev"), "错误信息应给出正确的开发启动方式");
    }

    @Test
    void productionRejectsWildcardAndNullCorsOrigins() {
        StartupSafetyCheck wildcard = newCheck(false, false, List.of("https://fm.example.com", "*"));
        IllegalStateException ex = assertThrows(IllegalStateException.class, wildcard::verifyProductionSafety);
        assertTrue(ex.getMessage().contains("[*]"), "错误信息应列出具体越界的来源");

        StartupSafetyCheck nullOrigin = newCheck(false, false, List.of("null"));
        assertThrows(IllegalStateException.class, nullOrigin::verifyProductionSafety);

        // 子域名通配在生产也禁止（要求完整的协议+域名）
        StartupSafetyCheck subdomain = newCheck(false, false, List.of("https://*.example.com"));
        IllegalStateException subEx = assertThrows(IllegalStateException.class, subdomain::verifyProductionSafety);
        assertTrue(subEx.getMessage().contains("https://*.example.com"), "错误信息应列出越界项");
    }

    @Test
    void productionAllowsConcreteOrEmptyOrigins() {
        assertDoesNotThrow(newCheck(false, false, List.of("https://fm.example.com"))::verifyProductionSafety);
        assertDoesNotThrow(newCheck(false, false, null)::verifyProductionSafety);
        // 空串与空白项不应被当成一个来源，也不应触发误报
        assertDoesNotThrow(newCheck(false, false, List.of("", "  "))::verifyProductionSafety);
    }

    /**
     * JWT 密钥：示例值 / 过短 / 未配置都属于不合格
     *
     * 为什么需要这一条：生产配置里 jwt.secret 无默认值，“没配”会因占位符解析失败而起不来，
     * 但“配了文档示例值”或“配了短密码”会静默跑起来，而这两种情况都能被伪造任意用户 token
     */
    @Test
    void productionRejectsWeakOrPlaceholderJwtSecret() {
        assertThrows(IllegalStateException.class,
                newCheck(false, false, List.of(), "short")::verifyProductionSafety);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                newCheck(false, false, List.of(), "your-256-bit-secret-key-change-in-production")::verifyProductionSafety);
        assertTrue(ex.getMessage().contains("JWT 密钥不合格"), "错误信息应指出问题类型");
        assertTrue(ex.getMessage().contains("openssl rand"), "错误信息应给出可执行的生成方式");
        assertThrows(IllegalStateException.class,
                newCheck(false, false, List.of(), "   ")::verifyProductionSafety);
    }

    /**
     * dev profile 不做 JWT 强度限制（开发用的是固定的短默认密钥）
     */
    @Test
    void devProfileSkipsJwtSecretCheck() {
        assertDoesNotThrow(newCheck(true, false, List.of(), "dev-default-secret-for-testing-only")::verifyProductionSafety);
    }

    /**
     * 回归：生产环境不得开启接口文档
     *
     * 背景：AGENTS.md 一直把 springdoc.*.enabled 列为"只能开在开发环境的开关"，
     *      但自检里并没有这一条 —— 只要有人线上设了 SWAGGER_ENABLED=true 又忘了关，
     *      全量接口清单就会长期对外公开。
     */
    @Test
    void productionRejectsEnabledApiDocs() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                newCheck(false, false, List.of(), VALID_SECRET, true)::verifyProductionSafety);
        assertTrue(ex.getMessage().contains("SWAGGER_ENABLED"), "错误信息应指出具体环境变量");
        assertTrue(ex.getMessage().contains("profiles=dev"), "错误信息应给出看文档的正确方式");

        // dev profile 下仍然允许（本地就是要看 swagger-ui）
        assertDoesNotThrow(newCheck(true, false, List.of(),
                "dev-default-secret-for-testing-only", true)::verifyProductionSafety);
    }
}
