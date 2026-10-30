package com.platform.api.auth;

import com.platform.api.common.ResultInfo;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    public AuthController(AuthService authService, CaptchaService captchaService) {
        this.authService = authService;
        this.captchaService = captchaService;
    }

    @PostMapping("/login")
    public ResultInfo<AuthDtos.LoginResult> login(@RequestBody AuthDtos.LoginRequest request) {
        return ResultInfo.ok(authService.login(request));
    }

    @GetMapping("/captcha")
    public ResultInfo<AuthDtos.CaptchaInfo> captcha() {
        return ResultInfo.ok(captchaService.create());
    }

    @PostMapping("/refresh-token")
    public ResultInfo<AuthDtos.LoginResult> refreshToken(@RequestParam(required = false) String refreshToken) {
        return ResultInfo.ok(authService.refresh(refreshToken));
    }

    @DeleteMapping("/logout")
    public ResultInfo<Void> logout() {
        return ResultInfo.ok();
    }
}
