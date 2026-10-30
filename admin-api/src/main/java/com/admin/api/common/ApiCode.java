package com.admin.api.common;

/**
 * 与前端 src/enums/api.ts 保持一致的业务响应码。
 */
public final class ApiCode {

    public static final String SUCCESS = "00000";
    public static final String ACCESS_TOKEN_INVALID = "A0230";
    public static final String REFRESH_TOKEN_INVALID = "A0231";
    public static final String PERMISSION_DENIED = "A0301";
    public static final String BAD_REQUEST = "A0400";
    public static final String NOT_FOUND = "A0404";
    public static final String SERVER_ERROR = "B0001";

    private ApiCode() {
    }
}
