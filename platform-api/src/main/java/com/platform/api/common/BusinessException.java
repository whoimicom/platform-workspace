package com.platform.api.common;

import org.springframework.http.HttpStatus;

/**
 * 可预期的业务异常，由全局异常处理器转换为统一响应。
 */
public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public BusinessException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(ApiCode.BAD_REQUEST, message, HttpStatus.BAD_REQUEST);
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(ApiCode.NOT_FOUND, message, HttpStatus.NOT_FOUND);
    }

    public static BusinessException unauthorized(String code, String message) {
        return new BusinessException(code, message, HttpStatus.UNAUTHORIZED);
    }
}
