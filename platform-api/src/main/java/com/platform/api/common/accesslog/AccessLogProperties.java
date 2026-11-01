package com.platform.api.common.accesslog;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 接口访问日志配置，前缀 {@code app.access-log}。
 */
@ConfigurationProperties(prefix = "app.access-log")
public class AccessLogProperties {

    /** 是否启用接口访问日志 */
    private boolean enabled = true;

    /** 请求/返回报文的最大记录长度，超出截断，防止大报文撑爆日志 */
    private int maxBodyLength = 4096;

    /** 慢请求阈值（毫秒），超过则以 WARN 级别输出 */
    private long slowThresholdMs = 3000;

    /** 不记录日志的路径（Ant 风格，匹配请求 URI） */
    private List<String> excludePaths = new ArrayList<>(List.of(
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/webjars/**",
            "/favicon.ico",
            "/error",
            "/api/realtime/ws/**"
    ));

    /** 报文中需要脱敏的字段名（JSON key 或表单字段），值会被替换为 ****** */
    private List<String> maskFields = new ArrayList<>(List.of(
            "password", "oldPassword", "newPassword", "confirmPassword",
            "secret", "appSecret", "accessToken", "refreshToken", "captchaCode"
    ));

    /** 是否记录 User-Agent */
    private boolean logUserAgent = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxBodyLength() {
        return maxBodyLength;
    }

    public void setMaxBodyLength(int maxBodyLength) {
        this.maxBodyLength = maxBodyLength;
    }

    public long getSlowThresholdMs() {
        return slowThresholdMs;
    }

    public void setSlowThresholdMs(long slowThresholdMs) {
        this.slowThresholdMs = slowThresholdMs;
    }

    public List<String> getExcludePaths() {
        return excludePaths;
    }

    public void setExcludePaths(List<String> excludePaths) {
        this.excludePaths = excludePaths;
    }

    public List<String> getMaskFields() {
        return maskFields;
    }

    public void setMaskFields(List<String> maskFields) {
        this.maskFields = maskFields;
    }

    public boolean isLogUserAgent() {
        return logUserAgent;
    }

    public void setLogUserAgent(boolean logUserAgent) {
        this.logUserAgent = logUserAgent;
    }
}
