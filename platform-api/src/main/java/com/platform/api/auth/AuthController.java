package com.platform.api.auth;

import com.platform.api.common.ApiResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    public AuthController(AuthService authService, CaptchaService captchaService) {
        this.authService = authService;
        this.captchaService = captchaService;
    }

    @PostMapping("/login")
    public ApiResult<AuthDtos.LoginResult> login(@RequestBody AuthDtos.LoginRequest request) {
        return ApiResult.ok(authService.login(request));
    }

    @GetMapping("/captcha")
    public ApiResult<AuthDtos.CaptchaInfo> captcha() {
        return ApiResult.ok(captchaService.create());
    }

    @PostMapping("/refresh-token")
    public ApiResult<AuthDtos.LoginResult> refreshToken(@RequestParam(required = false) String refreshToken) {
        return ApiResult.ok(authService.refresh(refreshToken));
    }

    @DeleteMapping("/logout")
    public ApiResult<Void> logout() {
        return ApiResult.ok();
    }
}
