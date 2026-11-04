package com.platform.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 三方登录（微信开放平台 / QQ 互联）配置。
 */
@ConfigurationProperties(prefix = "app.oauth")
public class OAuthProperties {

    /** 微信开放平台扫码登录 */
    private final WeChat wechat = new WeChat();

    /** QQ 互联网站应用授权登录 */
    private final Qq qq = new Qq();

    public WeChat getWechat() {
        return wechat;
    }

    public Qq getQq() {
        return qq;
    }

    /** 微信开放平台 OAuth2（snsapi_login） */
    public static class WeChat {

        /** 微信开放平台创建的网站应用 AppID；留空则 exchangeOpenId 退化为 mock */
        private String appId;

        /** AppSecret */
        private String appSecret;

        /** 授权回调域（与开放平台配置完全一致） */
        private String redirectUri;

        /** 是否启用（appId + appSecret 都配置才算启用） */
        public boolean isEnabled() {
            return appId != null && !appId.isBlank() && appSecret != null && !appSecret.isBlank();
        }

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getAppSecret() {
            return appSecret;
        }

        public void setAppSecret(String appSecret) {
            this.appSecret = appSecret;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }
    }

    /** QQ 互联（connect.qq.com）网站应用 */
    public static class Qq {

        /** 网站应用 AppID；留空则 exchangeOpenId 退化为 mock */
        private String appId;

        /** 应用密钥 AppKey */
        private String appSecret;

        /** 授权回调地址（需与 QQ 互联后台登记的回调地址完全一致） */
        private String redirectUri;

        /** 是否启用（appId + appSecret 都配置才算启用） */
        public boolean isEnabled() {
            return appId != null && !appId.isBlank() && appSecret != null && !appSecret.isBlank();
        }

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getAppSecret() {
            return appSecret;
        }

        public void setAppSecret(String appSecret) {
            this.appSecret = appSecret;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }
    }
}
