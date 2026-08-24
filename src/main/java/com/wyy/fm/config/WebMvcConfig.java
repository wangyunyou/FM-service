package com.wyy.fm.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类
 * 
 * 作用：注册拦截器、配置跨域、静态资源等
 * 
 * 注解说明：
 * - @Configuration：标记为配置类，Spring 启动时加载
 * - WebMvcConfigurer：Spring MVC 配置接口，可以重写各种方法
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    /**
     * 注册拦截器
     * 
     * 配置哪些路径需要拦截，哪些路径放行
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")  // 拦截所有 /api/ 开头的请求
                .excludePathPatterns(        // 排除这些路径（不需要登录）
                        "/api/user/wx-login",  // 登录接口
                        "/health"              // 健康检查
                );
    }
}
