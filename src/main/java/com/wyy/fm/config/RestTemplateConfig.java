package com.wyy.fm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate 配置
 * 
 * RestTemplate 是什么：
 * - Spring 提供的 HTTP 客户端工具
 * - 用来调用其他服务的接口（如微信 API）
 * - 类似前端的 axios/fetch
 * 
 * 使用示例：
 * restTemplate.getForObject("https://api.example.com/users", Map.class);
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 创建 RestTemplate Bean
     * 
     * 配置了超时时间，避免调用第三方接口时卡死
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));  // 连接超时：3 秒
        factory.setReadTimeout(Duration.ofSeconds(5));     // 读取超时：5 秒
        return new RestTemplate(factory);
    }
}
