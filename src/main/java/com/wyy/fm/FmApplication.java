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
        loadDotenv();
        SpringApplication.run(FmApplication.class, args);
    }

    /**
     * 在 Spring 容器启动前自动装载 .env 文件中的配置到系统属性中。
     * 
     * 为什么需要：在 IntelliJ IDEA 或 VS Code 中直接点 Run 启动时，IDE 默认不会读取根目录的 .env 文件，
     * 导致 Supabase 数据库连接、微信密钥等回落为默认值（如尝试连本地 postgres 并报错）。
     * 本方法在 main 执行前将 .env 读入 System Properties，确保在任何 IDE 中直接点击启动均能开箱即用。
     */
    private static void loadDotenv() {
        java.nio.file.Path envFile = java.nio.file.Paths.get(".env");
        if (!java.nio.file.Files.exists(envFile)) {
            // 兼容工作目录位于项目根或上一层目录的情况
            envFile = java.nio.file.Paths.get("FM/.env");
        }
        if (java.nio.file.Files.exists(envFile)) {
            try {
                java.util.List<String> lines = java.nio.file.Files.readAllLines(envFile, java.nio.charset.StandardCharsets.UTF_8);
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                        continue;
                    }
                    int eqIndex = line.indexOf('=');
                    String key = line.substring(0, eqIndex).trim();
                    String value = line.substring(eqIndex + 1).trim();
                    if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    if (System.getProperty(key) == null && System.getenv(key) == null) {
                        System.setProperty(key, value);
                    }
                }
            } catch (Exception e) {
                System.err.println("[FmApplication] 读取 .env 异常: " + e.getMessage());
            }
        }
    }
}