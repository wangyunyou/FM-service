package com.wyy.fm.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 启动期安全自检
 *
 * 作用：把「只允许在开发环境打开的便利开关」和 profile 绑死，防止误配置被带上生产。
 *
 * 为什么要 fail-fast 而不是打日志：
 * - 这些开关的默认值本身是安全的（mock=false、CORS 留空、swagger=false），
 *   危险来自本地调通后把 dev 的值抄进生产配置、或忘了改回来
 * - 一条 WARN 在生产日志里等于不存在，身份认证绕过这类问题必须启动即失败
 *
 * 校验范围（只覆盖"开发便利"与"密钥强度"类配置，不替代各配置类自身的行为）：
 * - wx.miniapp.mock-enabled：开启后任意 code 都能换 token
 * - app.cors.allowed-origin-patterns：通配符（含 `*`）与 "null" 只允许在开发环境用
 * - jwt.secret：为空、仍是文档示例值或 UTF-8 长度不足 32 字节（弱密钥可被离线爆破）
 *
 * 依赖关系：
 * - 只读取 Environment 与配置值，不注册任何 HTTP 组件，因此不影响接口行为
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupSafetyCheck {

    /**
     * 当前 Spring 环境，用于判断激活了哪些 profile
     */
    private final Environment environment;

    /**
     * 允许使用开发便利开关的 profile
     * - 与 application-dev.yml 对应；生产使用默认 profile（无 profile 或 prod）
     */
    static final String DEV_PROFILE = "dev";

    /**
     * 判断某条跨域来源是否只允许在开发环境使用
     * - "null"：以 file:// 打开的本地页面（浏览器发的 Origin 就是 null）
     * - 含 * 的通配：生产只允许写完整的协议+域名，连 https://*.example.com 也不放行
     *   （本项目生产就一两个固定域名，真需要通配时再显式改这里）
     */
    static boolean isDevOnlyOriginPattern(String pattern) {
        return "null".equals(pattern) || pattern.contains("*");
    }

    /**
     * JWT 密钥最小长度（UTF-8 字节）
     * - HMAC-SHA256 要求密钥不短于 256 bit，即 32 字节
     * - 按字节而不是 String.length() 判断：JwtUtil 签名时用的就是 getBytes(UTF_8)，
     *   度量口径必须和它一致，否则非 ASCII 密钥会判错
     */
    static final int MIN_JWT_SECRET_LENGTH = 32;

    /**
     * 文档/示例里出现过的占位密钥
     * - 生产误用等同于没有密钥，必须拦住
     */
    static final List<String> PLACEHOLDER_JWT_SECRETS = List.of(
            "your-256-bit-secret-key-change-in-production",
            "dev-default-secret-for-testing-only");

    @Value("${wx.miniapp.mock-enabled:false}")
    private boolean mockLoginEnabled;

    @Value("${app.cors.allowed-origin-patterns:#{null}}")
    private List<String> corsAllowedOriginPatterns;

    /**
     * JWT 签名密钥
     * - 生产 application.yml 要它无默认值（不配就启动失败），但示例值/弱密钥仍需这里拦截
     */
    @Value("${jwt.secret:}")
    private String jwtSecret;

    /**
     * 启动后立即校验，不通过则抛异常阻止容器起来
     *
     * @throws IllegalStateException 非 dev profile 下开启了开发便利配置或密钥不合格
     */
    @PostConstruct
    void verifyProductionSafety() {
        if (isDevProfile()) {
            log.info("开发环境自检跳过：mock 登录={}，跨域来源={}", mockLoginEnabled, normalize(corsAllowedOriginPatterns));
            return;
        }

        if (mockLoginEnabled) {
            throw new IllegalStateException(
                    "生产环境禁止开启微信登录 mock（wx.miniapp.mock-enabled=true / 环境变量 WX_MOCK_LOGIN=true）："
                            + "该开关会让任意 code 换取有效 token，等同关闭鉴权。请去掉该配置，或在开发环境用 -Dspring-boot.run.profiles=dev 启动");
        }

        List<String> unsafeOrigins = normalize(corsAllowedOriginPatterns).stream()
                .filter(StartupSafetyCheck::isDevOnlyOriginPattern)
                .collect(Collectors.toList());
        if (!unsafeOrigins.isEmpty()) {
            throw new IllegalStateException(
                    "生产环境跨域来源禁止通配符与 null（越界项：" + unsafeOrigins + "，配置项"
                            + " app.cors.allowed-origin-patterns / 环境变量 CORS_ALLOWED_ORIGIN_PATTERNS）："
                            + "请写完整的协议+域名，如 https://fm.example.com；留空即可（小程序原生请求不受浏览器同源策略限制，无需配置跨域）");
        }

        verifyJwtSecret();
    }

    /**
     * 校验 JWT 密钥强度
     *
     * 为什么生产需要：application.yml 里 jwt.secret 无默认值，“没配”会启动失败，
     * 但“配了文档里的示例值”或“配了个短密码”不会报错，
     * 而密钥泄露/可爆意味着任何人都能伪造任意用户 token
     */
    private void verifyJwtSecret() {
        String secret = jwtSecret == null ? "" : jwtSecret.trim();
        int secretBytes = secret.getBytes(StandardCharsets.UTF_8).length;
        if (secret.isEmpty() || PLACEHOLDER_JWT_SECRETS.contains(secret) || secretBytes < MIN_JWT_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "生产环境 JWT 密钥不合格（UTF-8 长度 " + secretBytes + " 字节，要求≥ " + MIN_JWT_SECRET_LENGTH
                            + " 字节且不得使用示例值）：请设置环境变量 JWT_SECRET 为随机生成的强密钥（如 `openssl rand -base64 48`），"
                            + "切勿把真实密钥写进代码或配置文件");
        }
    }

    /**
     * 是否运行在开发 profile
     */
    boolean isDevProfile() {
        return environment.acceptsProfiles(org.springframework.core.env.Profiles.of(DEV_PROFILE));
    }

    /**
     * 统一处理 null / 空串 / 空白项，避免把 "" 当成一个来源参与判断
     * - CorsConfig 与本类共用，避免同一套解析逻辑写两遍
     */
    static List<String> normalize(List<String> patterns) {
        return patterns == null ? List.of() :
                patterns.stream()
                        .map(String::trim)
                        .filter(pattern -> !pattern.isEmpty())
                        .collect(Collectors.toList());
    }
}
