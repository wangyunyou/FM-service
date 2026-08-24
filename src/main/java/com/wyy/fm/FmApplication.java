package com.wyy.fm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 启动类
 * 
 * 作用：应用程序入口，Spring Boot 从这里启动
 * 
 * 注解说明：
 * - @SpringBootApplication：组合注解，包含：
 *   - @Configuration：标记为配置类
 *   - @EnableAutoConfiguration：启用自动配置（根据依赖自动配置 Spring）
 *   - @ComponentScan：扫描当前包及子包下的所有组件（@Controller、@Service、@Repository 等）
 * 
 * 启动流程：
 * 1. 执行 main 方法
 * 2. SpringApplication.run() 启动 Spring 容器
 * 3. 扫描并注册所有 Bean（Controller、Service、Repository 等）
 * 4. 启动内嵌 Tomcat 服务器（默认端口 8080）
 * 5. 应用启动完成，可以接收请求
 */
@SpringBootApplication
public class FmApplication {

    /**
     * 主方法 — 应用程序入口
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(FmApplication.class, args);
    }
}
