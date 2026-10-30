package com.platform.api.security;

import com.platform.api.common.ApiCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 未认证 / 访问令牌失效时返回 A0230，前端据此触发令牌续期。
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        SecurityResponses.write(response, HttpStatus.UNAUTHORIZED,
                ApiCode.ACCESS_TOKEN_INVALID, "访问令牌无效或已过期");
    }
}
