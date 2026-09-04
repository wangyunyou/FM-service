package com.wyy.fm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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
     * 1. 配置超时时间，避免调用第三方接口时卡死
     * 2. 微信 code2Session 接口返回的 Content-Type 可能是 text/plain 或 text/html，
     *    实质为 JSON；为 Jackson 转换器添加 text/plain 支持，避免抛出 UnknownContentTypeException
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));  // 连接超时：3 秒
        factory.setReadTimeout(Duration.ofSeconds(5));     // 读取超时：5 秒
        RestTemplate restTemplate = new RestTemplate(factory);

        for (HttpMessageConverter<?> converter : restTemplate.getMessageConverters()) {
            if (converter instanceof MappingJackson2HttpMessageConverter jacksonConverter) {
                List<MediaType> supportedMediaTypes = new ArrayList<>(jacksonConverter.getSupportedMediaTypes());
                supportedMediaTypes.add(MediaType.TEXT_PLAIN);
                supportedMediaTypes.add(MediaType.TEXT_HTML);
                jacksonConverter.setSupportedMediaTypes(supportedMediaTypes);
            }
        }
        return restTemplate;
    }
}
