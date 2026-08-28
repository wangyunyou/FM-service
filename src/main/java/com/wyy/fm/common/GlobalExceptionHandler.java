package com.wyy.fm.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;
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
     * 请求体 JSON 解析失败（格式错误、字段类型不匹配、乱码）
     *
     * 为什么必须接：不接就落到兜底 Exception 分支变成 500，
     * 而这纯粹是客户端把请求体写错了；同时它默认的 message 里带 Java 类型与堆栈信息，
     * 直接透出去等于把实现细节写给用户看（前端 request 层会原样 toast）。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException ex) {
        log.warn("请求体解析失败: {}", ex.getMessage());
        return Result.fail(ErrorCode.BAD_REQUEST.getCode(), "请求体格式不正确，请检查字段类型");
    }

    /**
     * 把绑定/校验失败的字段错误拼成人读提示（两个 handler 共用，避免重复逻辑）
     *
     * 两类错误要分开处理：
     * - 校验失败（@NotNull/@Min 等）：getDefaultMessage() 就是我们写在注解上的中文，直接用
     * - 类型转换失败（如 startDate=2026-02-30 绑不进 LocalDate）：message 是 Spring 自己生成的
     *   "Failed to convert property value of type 'java.lang.String' ..."，
     *   实测会被前端逐字 toast 给用户，必须换成「字段名 + 格式不正确」
     *
     * @param ex BindException 及其子类（MethodArgumentNotValidException 在 Spring 6 中继承自 BindException）
     * @return 拼接后的字段错误提示（多个字段以分号连接）
     */
    private String fieldErrorsMessage(BindException ex) {
        return ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::fieldErrorMessage)
                .collect(Collectors.joining("; "));
    }

    /**
     * 单个字段错误 → 人读文案
     */
    private static String fieldErrorMessage(FieldError error) {
        if (error.isBindingFailure()) {
            return paramNameOf(error.getField()) + "格式不正确";
        }
        return error.getDefaultMessage();
    }

    /**
     * 单个参数类型转换失败（不是 DTO 绑定，而是方法入参本身）
     *
     * 为什么不接住就会错：
     * - `PUT /api/diet/abc` 这种路径参数不是数字，抛的是 MethodArgumentTypeMismatchException
     *   （既不是 BindException 也不是 IllegalArgumentException），会落到兜底 Exception 分支
     *   把应有的 400 变成 500「服务器内部错误」
     * - 另外 Spring 默认会把带注解全限定名的原始转换异常当参数错误抛给
     *   IllegalArgumentException 分支，直接把 `@jakarta.validation.constraints.NotNull java.time.LocalDate`
     *   这种内部文案透到用户眼前（前端 request 层会原样 toast）
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("参数类型错误: name={}, value={}", ex.getName(), ex.getValue());
        return Result.fail(ErrorCode.BAD_REQUEST.getCode(), paramNameOf(ex.getName()) + "格式不正确");
    }

    /**
     * 唯一约束冲突（并发注册、并发写入同一业务唯一键）
     *
     * 不接住的话会落到兜底分支，给前端返一个含义不明的 500。
     * 业务层能预见的冲突（如同一 openid 并发注册）已在 Service 里接住重试，
     * 这里是最后一道网，只记录不暴雲数据库细节。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("数据约束冲突", ex);
        return Result.fail(ErrorCode.BAD_REQUEST.getCode(), "数据已存在或已被引用，请刷新后重试");
    }

    /**
     * 把 Java 字段名转成给人看的参数名（startDate → 开始日期）
     *
     * 为什么需要：异常里只有 `name`（如 startDate / id），直接拼给用户会出
     * “startDate 格式不正确”这种开发词语。映射不到时回退原名，不会比现在更差。
     */
    private static final Map<String, String> PARAM_LABELS = Map.of(
            "startDate", "开始日期",
            "endDate", "结束日期",
            "recordDate", "记录日期",
            "id", "记录 ID");

    private static String paramNameOf(String name) {
        return PARAM_LABELS.getOrDefault(name, name);
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
     *
     * 注意：Spring 的参数转换失败也会落到本分支（MethodArgumentTypeMismatchException
     * 继承自 TypeMismatchException 而非本类，已在上面单独接住），
     * 自己代码里按项目约定不该抛 IllegalArgumentException（应用 BusinessException），
     * 这里只是防止意外堆栈直接变成 500。
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
