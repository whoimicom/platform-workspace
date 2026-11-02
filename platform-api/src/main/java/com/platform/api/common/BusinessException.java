package com.platform.api.common;

import org.springframework.http.HttpStatus;

/**
 * 可预期的业务异常，由全局异常处理器转换为统一响应。
 * 响应码直接沿用 HTTP 状态码，便于前端统一按状态判断。
 */
public class BusinessException extends RuntimeException {

    private final Integer code;
    private final HttpStatus status;

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.code = status.value();
        this.status = status;
    }

    public Integer getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(message, HttpStatus.BAD_REQUEST);
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(message, HttpStatus.NOT_FOUND);
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(message, HttpStatus.UNAUTHORIZED);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(message, HttpStatus.FORBIDDEN);
    }
}
