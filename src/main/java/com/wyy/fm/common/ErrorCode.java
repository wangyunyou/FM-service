package com.wyy.fm.common;

import lombok.Getter;

/**
 * 统一错误码枚举
 * 
 * 错误码规范：
 * - 200: 成功
 * - 400-499: 客户端错误
 * - 500-599: 服务端错误
 * - 1000-1999: 用户相关
 * - 2000-2999: 饮食记录相关
 * - 3000-3999: 第三方服务相关
 */
@Getter
public enum ErrorCode {

    // 通用错误
    SUCCESS(200, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 用户相关 1000-1999
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_DISABLED(1002, "账号已被禁用"),
    NO_PERMISSION(1003, "无权操作"),

    // 饮食记录相关 2000-2999
    DIET_RECORD_NOT_FOUND(2001, "饮食记录不存在"),
    DIET_DATE_INVALID(2002, "日期范围不合法"),

    // 第三方服务相关 3000-3999
    WX_LOGIN_FAILED(3001, "微信登录失败"),
    WX_API_ERROR(3002, "微信接口调用异常");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
