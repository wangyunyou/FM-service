package com.wyy.fm.common;

import lombok.Getter;

/**
 * 业务异常
 * 
 * 用于业务逻辑中的可预期错误，会返回对应的错误码和提示信息给前端
 * 
 * 使用示例：
 * - throw new BusinessException(ErrorCode.USER_NOT_FOUND);
 * - throw new BusinessException(ErrorCode.NO_PERMISSION, "该记录属于其他用户");
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }
}
