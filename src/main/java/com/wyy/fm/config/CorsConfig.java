package com.wyy.fm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS 跨域配置
 * 
 * 什么是跨域：
 * - 浏览器同源策略：协议+域名+端口 必须相同
 * - 前端在 localhost:3000，后端在 localhost:8080，属于跨域
 * - 小程序没有跨域问题，但 H5 需要
 * 
 * 作用：允许前端跨域访问后端接口
 */
@Configuration
public class CorsConfig {

    /**
     * 创建 CORS 过滤器
     * 
     * @Bean：标记为 Spring Bean，自动注册到容器
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");  // 允许所有来源（生产环境应该指定具体域名）
        config.addAllowedHeader("*");          // 允许所有请求头
        config.addAllowedMethod("*");          // 允许所有 HTTP 方法（GET/POST/PUT/DELETE）
        config.setAllowCredentials(true);     // 允许携带 Cookie
        config.setMaxAge(3600L);              // 预检请求缓存 1 小时

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);  // 对所有路径生效
        return new CorsFilter(source);
    }
}
