package com.platform.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 应用顶层配置。按模块拆分后，这里只保留全局开关与跨模块共享的默认值。
 * <p>
 * 各模块独立 Properties：
 * <ul>
 *   <li>{@link CaptchaProperties} — app.captcha 图形验证码签名配置（含 enabled 开关）</li>
 *   <li>{@link JwtProperties}     — app.jwt      JWT 令牌签名配置</li>
 *   <li>{@link OAuthProperties}   — app.oauth    三方登录（微信/QQ）配置</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** 新增用户 / SQL 初始账号 NEED_INIT 的默认密码 */
    private String defaultPassword = "123456";

    public String getDefaultPassword() {
        return defaultPassword;
    }

    public void setDefaultPassword(String defaultPassword) {
        this.defaultPassword = defaultPassword;
    }
}
