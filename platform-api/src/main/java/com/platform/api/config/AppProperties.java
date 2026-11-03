package com.platform.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** 是否启用图形验证码校验 */
    private boolean captchaEnabled = true;

    /** 图形验证码配置（签名 token 模式） */
    private final Captcha captcha = new Captcha();

    /** 新增用户 / SQL 初始账号的默认密码 */
    private String defaultPassword = "123456";

    private final Jwt jwt = new Jwt();

    public boolean isCaptchaEnabled() {
        return captchaEnabled;
    }

    public void setCaptchaEnabled(boolean captchaEnabled) {
        this.captchaEnabled = captchaEnabled;
    }

    public String getDefaultPassword() {
        return defaultPassword;
    }

    public void setDefaultPassword(String defaultPassword) {
        this.defaultPassword = defaultPassword;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public Captcha getCaptcha() {
        return captcha;
    }

    /** 图形验证码（签名 token + 指纹 + 本地防重放）配置 */
    public static class Captcha {

        /**
         * HMAC-SHA256 签名密钥，至少 32 字节；留空则退化为 app.jwt.secret。
         */
        private String secret;

        /** token 有效期（秒），默认 300 秒 */
        private long expireSeconds = 300;

        /** 本地防重放黑名单上限，默认 10 万条 */
        private int replayCacheMaxSize = 100_000;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpireSeconds() {
            return expireSeconds;
        }

        public void setExpireSeconds(long expireSeconds) {
            this.expireSeconds = expireSeconds;
        }

        public int getReplayCacheMaxSize() {
            return replayCacheMaxSize;
        }

        public void setReplayCacheMaxSize(int replayCacheMaxSize) {
            this.replayCacheMaxSize = replayCacheMaxSize;
        }
    }

    public static class Jwt {

        /** HMAC 密钥，至少 32 字节 */
        private String secret = "platform-api-default-secret-change-me-0123456789abcdef";

        /** 访问令牌有效期（秒） */
        private long accessExpireSeconds = 1800;

        /** 刷新令牌有效期（秒） */
        private long refreshExpireSeconds = 604800;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getAccessExpireSeconds() {
            return accessExpireSeconds;
        }

        public void setAccessExpireSeconds(long accessExpireSeconds) {
            this.accessExpireSeconds = accessExpireSeconds;
        }

        public long getRefreshExpireSeconds() {
            return refreshExpireSeconds;
        }

        public void setRefreshExpireSeconds(long refreshExpireSeconds) {
            this.refreshExpireSeconds = refreshExpireSeconds;
        }
    }
}
