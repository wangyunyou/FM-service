package com.wyy.fm.common;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 「传了就不能是空白」校验
 *
 * 为什么需要自定义注解（而不是直接用 @NotBlank）：
 * - 部分更新 DTO（UpdateXxxRequest）里的可选字段有三种语义：
 *   不传/null = 不改、空串 = 清空或非法、有值 = 覆盖
 * - @NotBlank 把 null 也判为失败，于是「不改这个字段」的合法请求会被误挡 400。
 *   实测过：给 UpdateDietRecordRequest.foodName 加 @NotBlank 之后，
 *   只改热量的 {"calories": -5} 返回的 message 变成"食物名称不能为空; 热量不能为负数"，
 *   而 {"remark":"加了糖"} 这种正常请求直接被拒 —— 破坏原有部分更新语义
 * - @Size(min=1) 同样不行：它挡不住纯空白，实测 {"foodName":"   "} 能 200 入库
 *
 * 所以这里只拦「非 null 但 trim 后为空」的值，null 直接放行。
 *
 * 使用示例：
 * <pre>
 * &#64;NotBlankIfPresent(message = "食物名称不能为空")
 * &#64;Size(max = 200, message = "食物名称最长 200 个字符")
 * private String foodName;   // 可选：null 表示不改
 * </pre>
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NotBlankIfPresentValidator.class)
public @interface NotBlankIfPresent {

    String message() default "不能为空白";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
