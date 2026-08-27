package com.wyy.fm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * CORS 跨域配置
 *
 * 什么是跨域：
 * - 浏览器同源策略：协议+域名+端口 必须相同
 * - 前端在 localhost:3000、后端在 localhost:8080，属于跨域
 *
 * 谁真正需要 CORS：
 * - 小程序原生请求（wx.request）不走浏览器同源策略，配不配都能通
 * - 只有 H5 页面、本地浏览器调试（含仓库里的 test-api.html）需要放行
 *
 * 安全口径：
 * - 允许来源改成配置驱动，不再硬编码 "*"
 * - "*" 与 allowCredentials(true) 同时开启，等于允许任意站点带 Cookie 访问，
 *   所以只有显式配了来源才开启凭证支持；未配置即不放行任何跨域来源
 */
@Configuration
public class CorsConfig {

    /**
     * 允许跨域的来源（逗号分隔，支持 * 通配）
     * - 生产：环境变量 CORS_ALLOWED_ORIGIN_PATTERNS，例如 https://fm.example.com
     * - 开发：application-dev.yml 里放开 localhost 任意端口
     * - 留空：不放行任何浏览器跨域来源（小程序侧无影响）
     */
    @Value("${app.cors.allowed-origin-patterns:#{null}}")
    private List<String> allowedOriginPatterns;

    /**
     * 创建 CORS 过滤器
     *
     * @Bean：标记为 Spring Bean，由 Spring Boot 自动注册进 Servlet 过滤器链
     */
    @Bean
    public CorsFilter corsFilter() {
        // 来源解析与启动自检共用（空串/空白项过滤），见 StartupSafetyCheck.normalize
        List<String> origins = StartupSafetyCheck.normalize(allowedOriginPatterns);

        CorsConfiguration config = new CorsConfiguration();
        if (!origins.isEmpty()) {
            origins.forEach(config::addAllowedOriginPattern);
            // 允许携带 Cookie：仅在已收敛到具体来源时开启
            config.setAllowCredentials(true);
        }
        config.addAllowedHeader("*");    // 允许所有请求头（含 Authorization）
        config.addAllowedMethod("*");    // 允许所有 HTTP 方法（GET/POST/PUT/DELETE）
        config.setMaxAge(3600L);         // 预检请求（OPTIONS）结果缓存 1 小时，减少往返

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);  // 对所有路径生效
        return new CorsFilter(source);
    }
}
