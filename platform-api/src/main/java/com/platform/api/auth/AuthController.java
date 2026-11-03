package com.platform.api.auth;

import com.platform.api.common.ResultInfo;
import jakarta.servlet.http.HttpServletRequest;
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
    private final OAuthService oauthService;
    private final SmsCodeService smsCodeService;

    public AuthController(AuthService authService,
                          CaptchaService captchaService,
                          OAuthService oauthService,
                          SmsCodeService smsCodeService) {
        this.authService = authService;
        this.captchaService = captchaService;
        this.oauthService = oauthService;
        this.smsCodeService = smsCodeService;
    }

    // ---- 账号密码 ----

    @PostMapping("/login")
    public ResultInfo<AuthDtos.LoginResult> login(@RequestBody AuthDtos.LoginRequest request,
                                                  HttpServletRequest httpRequest) {
        return ResultInfo.ok(authService.login(request, httpRequest));
    }

    @GetMapping("/captcha")
    public ResultInfo<AuthDtos.CaptchaInfo> captcha(HttpServletRequest request) {
        return ResultInfo.ok(captchaService.create(request));
    }

    @PostMapping("/refresh-token")
    public ResultInfo<AuthDtos.LoginResult> refreshToken(@RequestParam(required = false) String refreshToken) {
        return ResultInfo.ok(authService.refresh(refreshToken));
    }

    @DeleteMapping("/logout")
    public ResultInfo<Void> logout() {
        return ResultInfo.ok();
    }

    // ---- 短信验证码 ----

    @PostMapping("/sms/send")
    public ResultInfo<Void> sendSmsCode(@RequestParam String mobile,
                                        @RequestParam(required = false) String captchaToken,
                                        @RequestParam(required = false) String captchaCode,
                                        HttpServletRequest request) {
        smsCodeService.send(mobile, request, captchaToken, captchaCode);
        return ResultInfo.ok();
    }

    @PostMapping("/sms/login")
    public ResultInfo<AuthDtos.LoginResult> smsLogin(@RequestBody AuthDtos.SmsLoginRequest request) {
        return ResultInfo.ok(oauthService.smsDirectLogin(request.mobile(), request.smsCode()));
    }

    // ---- 三方登录 ----

    /** 前端内嵌二维码所需配置（appId / corpId / agentId，不含 secret） */
    @GetMapping("/oauth/client-config")
    public ResultInfo<AuthDtos.OauthClientConfig> clientConfig() {
        return ResultInfo.ok(oauthService.clientConfig());
    }

    /** 获取三方授权页地址（前端跳转扫码/授权，回调携带 code + state） */
    @GetMapping("/oauth/authorize-url")
    public ResultInfo<AuthDtos.OauthAuthorizeUrl> authorizeUrl(@RequestParam String provider) {
        return ResultInfo.ok(oauthService.buildAuthorizeUrl(provider));
    }

    @PostMapping("/oauth/login")
    public ResultInfo<AuthDtos.OauthLoginResult> oauthLogin(@RequestBody OauthLoginQuery query) {
        return ResultInfo.ok(oauthService.oauthLogin(query.provider(), query.appId(), query.code()));
    }

    @PostMapping("/oauth/bind")
    public ResultInfo<AuthDtos.LoginResult> oauthBind(@RequestBody AuthDtos.OauthBindRequest request) {
        return ResultInfo.ok(oauthService.bindAndLogin(request.bindToken(), request.mobile(), request.smsCode()));
    }

    public record OauthLoginQuery(String provider, String appId, String code) {
    }
}
