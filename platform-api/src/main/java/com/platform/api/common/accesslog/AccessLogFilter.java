package com.platform.api.common.accesslog;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

/**
 * 接口访问日志采集过滤器。
 *
 * <p>职责：生成/透传 traceId 并写入 MDC、包装请求与响应以缓存报文、
 * 统计耗时，请求结束后把快照交给 {@link AccessLogAsyncLogger} 异步输出。</p>
 *
 * <p>是否跳过记录由 {@link AccessLogInterceptor} 根据 {@link IgnoreAccessLog}
 * 注解写入请求属性决定。</p>
 */
public class AccessLogFilter extends OncePerRequestFilter {

    /** MDC 与请求属性中的 traceId 键名 */
    public static final String TRACE_ID_KEY = "traceId";
    /** 透传 traceId 的请求/响应头 */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    /** 跳过日志标记的请求属性键 */
    public static final String SKIP_ATTRIBUTE = AccessLogFilter.class.getName() + ".SKIP";
    /** 登录用户名属性键 */
    public static final String USERNAME_ATTRIBUTE = AccessLogFilter.class.getName() + ".USERNAME";

    private final AccessLogProperties properties;
    private final AccessLogAsyncLogger asyncLogger;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public AccessLogFilter(AccessLogProperties properties, AccessLogAsyncLogger asyncLogger) {
        this.properties = properties;
        this.asyncLogger = asyncLogger;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return true;
        }
        String uri = request.getRequestURI();
        for (String pattern : properties.getExcludePaths()) {
            if (pathMatcher.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        MDC.put(TRACE_ID_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        long startNanos = System.nanoTime();

        // multipart/二进制上传不缓存请求体，避免大文件占用内存；
        // 缓存上限取配置长度的两倍（下限 4KB），超出部分只透传不缓存
        int cacheLimit = Math.max(properties.getMaxBodyLength() * 2, 4096);
        ContentCachingRequestWrapper requestWrapper = isBinaryRequest(request)
                ? null
                : new ContentCachingRequestWrapper(request, cacheLimit);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        HttpServletRequest requestToUse = requestWrapper != null ? requestWrapper : request;
        try {
            filterChain.doFilter(requestToUse, responseWrapper);
        } finally {
            try {
                long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
                if (!Boolean.TRUE.equals(request.getAttribute(SKIP_ATTRIBUTE))) {
                    byte[] requestBody = requestWrapper != null
                            ? requestWrapper.getContentAsByteArray()
                            : new byte[0];
                    AccessLogRecord record = new AccessLogRecord(
                            traceId,
                            request.getMethod(),
                            request.getRequestURI(),
                            request.getQueryString(),
                            resolveClientIp(request),
                            request.getHeader("User-Agent"),
                            usernameOf(request),
                            responseWrapper.getStatus(),
                            elapsedMs,
                            request.getContentType(),
                            requestBody,
                            responseWrapper.getContentType(),
                            responseWrapper.getContentAsByteArray()
                    );
                    asyncLogger.log(record);
                }
            } catch (Throwable ignored) {
                // 日志采集失败绝不能影响请求
            } finally {
                MDC.remove(TRACE_ID_KEY);
                responseWrapper.copyBodyToResponse();
            }
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String header = request.getHeader(TRACE_ID_HEADER);
        if (StringUtils.hasText(header) && header.length() <= 64
                && header.chars().allMatch(ch -> Character.isLetterOrDigit(ch) || ch == '-')) {
            return header;
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private boolean isBinaryRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType == null) {
            return false;
        }
        String type = contentType.toLowerCase(Locale.ROOT);
        return type.startsWith("multipart/") || type.contains("octet-stream");
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return StringUtils.hasText(realIp) ? realIp : request.getRemoteAddr();
    }

    private String usernameOf(HttpServletRequest request) {
        Object username = request.getAttribute(USERNAME_ATTRIBUTE);
        return username instanceof String name ? name : null;
    }
}
