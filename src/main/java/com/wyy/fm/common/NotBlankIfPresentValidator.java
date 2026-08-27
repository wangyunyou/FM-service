package com.wyy.fm.common;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * {@link NotBlankIfPresent} 的校验实现
 *
 * 规则：null 放行（表示"不改这个字段"），非 null 时要求至少含一个非空白字符。
 *
 * 为什么单独成文件而不是写成注解的嵌套类：
 * Java 里注解的属性值在注解声明处就要能解析到类型，
 * 嵌套在自己体内的类写在注解后面会直接编译不过（找不到符号）。
 */
public class NotBlankIfPresentValidator
        implements ConstraintValidator<NotBlankIfPresent, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || !value.isBlank();
    }
}
