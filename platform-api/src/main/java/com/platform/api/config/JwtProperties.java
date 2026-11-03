package com.platform.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 令牌配置。
 */
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

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
