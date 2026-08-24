package com.wyy.fm.controller;

import com.wyy.fm.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 健康检查控制器
 * 
 * 作用：验证服务是否正常启动（运维/监控用）
 * 
 * 接口：
 * - GET /health：返回服务状态
 * 
 * 使用场景：
 * - 负载均衡器定期检查后端服务是否存活
 * - 监控系统判断服务是否正常
 * - 部署时验证服务启动成功
 */
@RestController  // @RestController = @Controller + @ResponseBody（返回 JSON 而不是页面）
public class HealthController {

    /**
     * 健康检查接口
     * 
     * 请求：GET /health
     * 响应：{"code":200, "message":"success", "data":{"status":"UP"}}
     * 
     * 注解说明：
     * - @GetMapping("/health")：映射 GET 请求到 /health 路径
     */
    @GetMapping("/health")
    public Result<Map<String, String>> health() {
        // Map.of()：Java 9+ 创建不可变 Map 的快捷方法
        return Result.ok(Map.of("status", "UP"));
    }

    /**
     * 版本信息接口
     */
    @GetMapping("/version")
    public Result<Map<String, String>> version() {
        return Result.ok(Map.of(
                "version", "1.0.0",
                "build", "2026-08-24"
        ));
    }
}
