package com.platform.api.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;

public final class SecurityResponses {

    private SecurityResponses() {
    }

    public static void write(HttpServletResponse response, HttpStatus status, String code, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String body = "{\"code\":\"" + code + "\",\"data\":null,\"msg\":\"" + message + "\"}";
        response.getWriter().write(body);
    }
}
