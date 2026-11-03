package com.platform.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 三方登录（微信开放平台 / 企业微信）配置。
 */
@ConfigurationProperties(prefix = "app.oauth")
public class OAuthProperties {

    /** 微信开放平台扫码登录 */
    private final WeChat wechat = new WeChat();

    /** 企业微信（自建应用授权） */
    private final WeCom wecom = new WeCom();

    public WeChat getWechat() {
        return wechat;
    }

    public WeCom getWecom() {
        return wecom;
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

    /** 企业微信自建应用 OAuth2 */
    public static class WeCom {

        /** 企业 CorpID */
        private String corpId;

        /** 自建应用 AgentId（数字） */
        private String agentId;

        /** 应用 Secret */
        private String appSecret;

        /** 授权回调地址（前端登录页完整 URL，需与企业微信后台可信域名一致） */
        private String redirectUri;

        /** 是否启用 */
        public boolean isEnabled() {
            return corpId != null && !corpId.isBlank() && appSecret != null && !appSecret.isBlank();
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }

        public String getCorpId() {
            return corpId;
        }

        public void setCorpId(String corpId) {
            this.corpId = corpId;
        }

        public String getAgentId() {
            return agentId;
        }

        public void setAgentId(String agentId) {
            this.agentId = agentId;
        }

        public String getAppSecret() {
            return appSecret;
        }

        public void setAppSecret(String appSecret) {
            this.appSecret = appSecret;
        }
    }
}
