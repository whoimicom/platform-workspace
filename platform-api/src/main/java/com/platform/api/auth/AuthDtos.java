package com.platform.api.auth;

/**
 * 与前端 src/api/auth/types.ts 对应的认证 DTO。
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(String username, String password,
                               String captchaId, String captchaCode, Boolean rememberMe) {
    }

    public record LoginResult(String accessToken, String refreshToken, String tokenType, long expiresIn) {
    }

    public record CaptchaInfo(String captchaId, String captchaBase64) {
    }
}
