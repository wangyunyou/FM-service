package com.wyy.fm.common;

import lombok.Data;

/**
 * 统一 API 响应封装
 * 
 * 作用：所有接口返回统一格式，前端处理更方便
 * 
 * 格式：
 * {
 *   "code": 200,        // 状态码（200 成功，其他失败）
 *   "message": "success", // 提示信息
 *   "data": { ... }     // 实际数据（失败时为 null）
 * }
 * 
 * 使用示例：
 * - 成功：return Result.ok(user);  // {code:200, message:"success", data:user}
 * - 失败：return Result.fail("用户不存在");  // {code:500, message:"用户不存在", data:null}
 * - 失败（指定错误码）：return Result.fail(1001, "用户不存在");
 * 
 * 泛型说明：
 * - <T>：泛型，data 可以是任何类型（User、List<DietRecord>、Map 等）
 * - Result<User>：data 是 User 对象
 * - Result<List<DietRecord>>：data 是饮食记录列表
 */
@Data
public class Result<T> {

    /**
     * 状态码
     * - 200：成功
     * - 400：参数错误
     * - 401：未登录
     * - 500：服务器内部错误
     * - 1001-1999：用户相关错误
     * - 2000-2999：饮食记录相关错误
     * - 3000-3999：第三方服务相关错误
     */
    private int code;

    /**
     * 提示信息
     * - 成功时："success"
     * - 失败时：具体错误信息（如"用户不存在"）
     */
    private String message;

    /**
     * 实际数据
     * - 成功时：返回的业务数据
     * - 失败时：null
     */
    private T data;

    /**
     * 构造函数私有化
     * - 外部不能直接 new Result()
     * - 只能通过下面的静态方法创建（保证格式统一）
     */
    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功响应（带数据）
     * 
     * 使用示例：
     * return Result.ok(user);
     * // 返回：{code:200, message:"success", data:{id:1, nickname:"张三"}}
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "success", data);
    }

    /**
     * 成功响应（无数据）
     * 
     * 使用示例：
     * return Result.ok();
     * // 返回：{code:200, message:"success", data:null}
     * 
     * 适用场景：
     * - 删除操作（不需要返回数据）
     * - 更新操作（不需要返回数据）
     */
    public static <T> Result<T> ok() {
        return new Result<>(200, "success", null);
    }

    /**
     * 失败响应（指定错误码和提示信息）
     * 
     * 使用示例：
     * return Result.fail(1001, "用户不存在");
     * // 返回：{code:1001, message:"用户不存在", data:null}
     */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 失败响应（默认 500 错误码）
     * 
     * 使用示例：
     * return Result.fail("服务器内部错误");
     * // 返回：{code:500, message:"服务器内部错误", data:null}
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }
}
