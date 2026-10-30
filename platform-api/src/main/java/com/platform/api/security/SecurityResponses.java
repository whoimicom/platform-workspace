package com.platform.api.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;

/**
 * 安全过滤器链内的统一 JSON 响应，结构对齐 ResultInfo。
 */
public final class SecurityResponses {

    private SecurityResponses() {
    }

    public static void write(HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String body = "{\"success\":false,\"resultCode\":" + status.value()
                + ",\"resultMsg\":\"" + message + "\"}";
        response.getWriter().write(body);
    }
}
