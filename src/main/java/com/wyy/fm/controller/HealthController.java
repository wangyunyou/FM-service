package com.wyy.fm.controller;

import com.wyy.fm.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 健康检查 — 验证服务是否正常启动
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public Result<Map<String, String>> health() {
        return Result.ok(Map.of("status", "UP"));
    }

    // 练习：加一个版本号接口
    @GetMapping("/version")
    public Result<Map<String, String>> version() {
        return Result.ok(Map.of(
            "version", "1.0.0",
            "build", "2026-08-24"
        ));
    }

    // 练习：带路径参数的接口
    @GetMapping("/greet/{name}")
    public Result<Map<String, String>> greet(
        @PathVariable String name  // 从 URL 路径取参数
    ) {
        return Result.ok(Map.of(
            "message", "Hello, " + name + "!"
        ));
    }

    // 练习：带查询参数的接口
    @GetMapping("/add")
    public Result<Map<String, Object>> add(
        @RequestParam Integer a,  // ?a=1&b=2
        @RequestParam Integer b
    ) {
        return Result.ok(Map.of(
            "a", a,
            "b", b,
            "sum", a + b
        ));
    }
}
