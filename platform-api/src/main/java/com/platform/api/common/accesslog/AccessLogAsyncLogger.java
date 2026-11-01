package com.platform.api.common.accesslog;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 异步接口日志输出。
 *
 * <p>报文解码、脱敏、截断、格式化全部在独立线程中完成；队列满时直接丢弃，
 * 保证日志永远不影响接口性能与可用性。日志名固定为 {@code access-log}，
 * 需要单独落文件时可在 logback 中按 logger 名路由。</p>
 */
@Component
public class AccessLogAsyncLogger {

    private static final Logger ACCESS_LOG = LoggerFactory.getLogger("access-log");
    private static final Logger SELF_LOG = LoggerFactory.getLogger(AccessLogAsyncLogger.class);
    private static final String MASK = "******";

    private final AccessLogProperties properties;
    private final ThreadPoolExecutor executor;
    private final Pattern jsonMaskPattern;
    private final Pattern formMaskPattern;

    public AccessLogAsyncLogger(AccessLogProperties properties) {
        this.properties = properties;
        this.executor = new ThreadPoolExecutor(
                1, 1,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(8192),
                runnable -> {
                    Thread thread = new Thread(runnable, "access-log-writer");
                    thread.setDaemon(true);
                    return thread;
                },
                // 队列满直接丢弃，日志绝不能阻塞或拖垮业务
                new ThreadPoolExecutor.DiscardPolicy()
        );
        String fields = String.join("|", properties.getMaskFields().stream()
                .map(Pattern::quote).toList());
        this.jsonMaskPattern = fields.isEmpty() ? null
                : Pattern.compile("(\"" + "(?:" + fields + ")" + "\"\\s*:\\s*\")[^\"]*",
                        Pattern.CASE_INSENSITIVE);
        this.formMaskPattern = fields.isEmpty() ? null
                : Pattern.compile("((?:" + fields + ")=)[^&\\s]*", Pattern.CASE_INSENSITIVE);
    }

    /**
     * 提交一条访问日志，异步格式化输出；任何异常都不允许抛回调用方。
     */
    public void log(AccessLogRecord record) {
        try {
            executor.execute(() -> write(record));
        } catch (Throwable ex) {
            SELF_LOG.debug("访问日志提交失败，已忽略: {}", ex.getMessage());
        }
    }

    private void write(AccessLogRecord record) {
        try {
            String requestBody = formatBody(record.requestBody(), record.requestContentType());
            String responseBody = formatBody(record.responseBody(), record.responseContentType());

            String message = String.format(
                    "[ACCESS] traceId=%s %s %s%s status=%d elapsed=%dms user=%s ip=%s%s req=%s resp=%s",
                    record.traceId(),
                    record.method(),
                    record.uri(),
                    record.queryString() == null ? "" : "?" + record.queryString(),
                    record.status(),
                    record.elapsedMs(),
                    record.username() == null ? "-" : record.username(),
                    record.clientIp(),
                    userAgentPart(record),
                    requestBody,
                    responseBody
            );

            if (record.elapsedMs() >= properties.getSlowThresholdMs()) {
                ACCESS_LOG.warn("[SLOW] {}", message);
            } else {
                ACCESS_LOG.info(message);
            }
        } catch (Throwable ex) {
            SELF_LOG.debug("访问日志输出失败，已忽略: {}", ex.getMessage());
        }
    }

    private String userAgentPart(AccessLogRecord record) {
        if (!properties.isLogUserAgent() || record.userAgent() == null) {
            return "";
        }
        String ua = record.userAgent();
        if (ua.length() > 100) {
            ua = ua.substring(0, 100);
        }
        return " ua=" + ua;
    }

    /**
     * 只在异步线程中执行：二进制不读内容，文本报文解码后脱敏并截断。
     */
    private String formatBody(byte[] body, String contentType) {
        if (body == null || body.length == 0) {
            return "-";
        }
        if (!isTextual(contentType)) {
            return "[binary " + body.length + " bytes]";
        }
        String text = new String(body, charsetOf(contentType));
        text = mask(text);
        int max = properties.getMaxBodyLength();
        if (max > 0 && text.length() > max) {
            return text.substring(0, max) + "...(truncated, total=" + text.length() + ")";
        }
        return text;
    }

    private String mask(String text) {
        if (jsonMaskPattern != null && text.contains("\"")) {
            text = jsonMaskPattern.matcher(text).replaceAll("$1" + MASK);
        }
        if (formMaskPattern != null && text.contains("=")) {
            text = formMaskPattern.matcher(text).replaceAll("$1" + MASK);
        }
        return text;
    }

    private boolean isTextual(String contentType) {
        if (contentType == null) {
            return true;
        }
        String type = contentType.toLowerCase(Locale.ROOT);
        return type.contains("json")
                || type.contains("text")
                || type.contains("xml")
                || type.contains("x-www-form-urlencoded")
                || type.contains("javascript");
    }

    private Charset charsetOf(String contentType) {
        if (contentType != null) {
            for (String part : contentType.split(";")) {
                String trimmed = part.trim();
                if (trimmed.toLowerCase(Locale.ROOT).startsWith("charset=")) {
                    try {
                        return Charset.forName(trimmed.substring(8).trim());
                    } catch (Exception ignored) {
                        // 非法 charset 按 UTF-8 处理
                    }
                }
            }
        }
        return StandardCharsets.UTF_8;
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}
