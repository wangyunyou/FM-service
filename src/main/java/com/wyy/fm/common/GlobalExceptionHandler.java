package com.wyy.fm.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常（可预期的业务错误）
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBusinessException(BusinessException ex) {
        log.warn("业务异常: code={}, message={}", ex.getErrorCode().getCode(), ex.getMessage());
        return Result.fail(ex.getErrorCode().getCode(), ex.getMessage());
    }

    /**
     * 参数校验异常（@RequestBody + @Valid 校验失败）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidation(MethodArgumentNotValidException ex) {
        return Result.fail(ErrorCode.BAD_REQUEST.getCode(), fieldErrorsMessage(ex));
    }

    /**
     * 参数绑定校验异常（GET 查询参数映射到 DTO 且字段上有 @NotNull 等规则时失败）
     *
     * 为什么必须单独处理：
     * - @RequestBody 上的 @Valid 失败抛 MethodArgumentNotValidException
     * - URL 查询参数（如 /api/diet/query?startDate=xxx）绑定失败抛的是 BindException
     * 不接住就会落到兜底 Exception 分支，把应有的 400 变成 500
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException ex) {
        return Result.fail(ErrorCode.BAD_REQUEST.getCode(), fieldErrorsMessage(ex));
    }

    /**
     * 把绑定/校验失败的字段错误拼成人读提示（两个 handler 共用，避免重复逻辑）
     *
     * @param ex BindException 及其子类（MethodArgumentNotValidException 在 Spring 6 中继承自 BindException）
     * @return 拼接后的字段错误提示（多个字段以分号连接）
     */
    private String fieldErrorsMessage(BindException ex) {
        return ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
    }

    /**
     * 资源未找到异常（404）
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoResourceFound(NoResourceFoundException ex) {
        return Result.fail(ErrorCode.NOT_FOUND.getCode(), ErrorCode.NOT_FOUND.getMessage());
    }

    /**
     * 请求方式不支持异常（405）
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return Result.fail(HttpStatus.METHOD_NOT_ALLOWED.value(), "不支持的请求方式: " + ex.getMethod());
    }

    /**
     * 参数错误（兜底）
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("参数错误: {}", ex.getMessage());
        return Result.fail(ErrorCode.BAD_REQUEST.getCode(), ex.getMessage());
    }

    /**
     * 未知异常（兜底）
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception ex) {
        log.error("未处理异常", ex);
        return Result.fail(ErrorCode.INTERNAL_ERROR.getCode(), ErrorCode.INTERNAL_ERROR.getMessage());
    }
}
