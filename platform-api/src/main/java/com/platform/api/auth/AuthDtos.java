package com.platform.api.auth;

/**
 * 与前端 src/api/auth/types.ts 对应的认证 DTO。
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(String username, String password,
                               String captchaToken, String captchaCode, Boolean rememberMe) {
    }

    public record LoginResult(String accessToken, String refreshToken, String tokenType, long expiresIn) {
    }

    public record CaptchaInfo(String captchaToken, String captchaBase64) {
    }

    // ---- 三方登录 ----

    /** 三方身份（mock 下由 code 直接映射而来） */
    public record OauthIdentity(
            String provider,   // WECHAT / QQ
            String appId,      // 可空
            String unionId,    // 可空
            String openId,
            String nickname,   // 可空
            String avatar      // 可空
    ) {
    }

    /**
     * 三方 code 换登录态的两种结果：
     * <ul>
     *   <li>已绑定 → loggedIn=true，携带 accessToken</li>
     *   <li>未绑定 → needBind=true，前端拿着 bindToken 去手机号验证码绑定</li>
     * </ul>
     */
    public record OauthLoginResult(
            boolean loggedIn,
            boolean needBind,
            String accessToken,
            String refreshToken,
            String bindToken
    ) {
        public static OauthLoginResult loggedIn(LoginResult result) {
            return new OauthLoginResult(true, false,
                    result.accessToken(), result.refreshToken(), null);
        }

        public static OauthLoginResult needBind(String bindToken) {
            return new OauthLoginResult(false, true, null, null, bindToken);
        }
    }

    /** 三方授权页地址（前端打开后扫码/授权，回调携带 code + state） */
    public record OauthAuthorizeUrl(String url, String state) {
    }

    /**
     * 前端内嵌二维码所需的公开配置（不含任何 secret）。
     */
    public record OauthClientConfig(
            ProviderConfig wechat,
            ProviderConfig qq
    ) {
        /**
         * @param appId       微信为开放平台 AppID；QQ 为 QQ 互联 AppID
         * @param redirectUri 授权回调地址（前端内嵌二维码时传给 SDK）
         * @param enabled     是否已完成配置，可用于前端判断是否展示入口
         */
        public record ProviderConfig(String appId, String redirectUri, boolean enabled) {
        }
    }

    /** 三方绑定登录请求 */
    public record OauthBindRequest(String bindToken, String mobile, String smsCode) {
    }

    /** 手机号验证码直接登录请求 */
    public record SmsLoginRequest(String mobile, String smsCode) {
    }
}
