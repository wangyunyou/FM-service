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
     * 拦截范围只写了 /api/**，因此：
     * - /health、/version 本来就不在拦截路径内，不需要再加 exclude（历史上的冗余配置，已删）
     * - 新增公开接口时，必须放在 /api/** 下并加到 excludePathPatterns，
     *   或者放在 /api/** 之外（后者更容易忘记鉴权，推荐前者）
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")  // 拦截所有 /api/ 开头的请求
                .excludePathPatterns(        // 排除这些路径（不需要登录）
                        "/api/user/wx-login"  // 登录接口：此时还没有 token
                );
    }
}
