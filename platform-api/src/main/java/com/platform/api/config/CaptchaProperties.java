package com.platform.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 图形验证码（签名 token + 指纹 + 本地防重放）配置。
 */
@ConfigurationProperties(prefix = "app.captcha")
public class CaptchaProperties {

    /** 是否启用验证码校验（全局开关，可通过环境变量覆盖） */
    private boolean enabled = true;

    /** HMAC-SHA256 签名密钥，至少 32 字节；留空则退化为 app.jwt.secret */
    private String secret;

    /** token 有效期（秒），默认 300 秒 */
    private long expireSeconds = 300;

    /** 本地防重放黑名单容量上限，默认 10 万条 */
    private int replayCacheMaxSize = 100_000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

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
