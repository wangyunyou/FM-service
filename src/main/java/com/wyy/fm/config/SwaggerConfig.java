package com.wyy.fm.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI 配置
 * 
 * 访问地址：
 * - Swagger UI：http://localhost:8080/swagger-ui.html
 * - OpenAPI JSON：http://localhost:8080/v3/api-docs
 * 
 * 作用：
 * - 自动生成 API 文档
 * - 在线测试接口
 * - 前端可以根据文档生成代码
 */
@Configuration
public class SwaggerConfig {

    /**
     * 配置 OpenAPI 文档信息
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FM 小程序后端 API")
                        .version("1.0.0")
                        .description("饮食记录管理系统的后端接口文档")
                        .contact(new Contact()
                                .name("开发团队")
                                .email("dev@example.com")))
                // 全局安全配置：所有接口都需要 Bearer Token
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT 认证，格式：Bearer {token}")));
    }
}
