package com.admin.api.common;

/**
 * 统一响应结构：{ code, data, msg }。
 */
public record ApiResult<T>(String code, T data, String msg) {

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(ApiCode.SUCCESS, data, "一切ok");
    }

    public static <T> ApiResult<T> ok(String msg, T data) {
        return new ApiResult<>(ApiCode.SUCCESS, data, msg);
    }

    public static ApiResult<Void> ok() {
        return new ApiResult<>(ApiCode.SUCCESS, null, "一切ok");
    }

    public static <T> ApiResult<T> fail(String code, String msg) {
        return new ApiResult<>(code, null, msg);
    }
}
