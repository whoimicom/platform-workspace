package com.platform.api.common.accesslog;

/**
 * 一次接口访问的日志快照。
 *
 * <p>报文以字节数组传递，解码、截断、脱敏都在异步线程中完成，
 * 请求线程只负责采集，避免日志处理影响接口性能。</p>
 *
 * @param traceId             链路 ID
 * @param method              HTTP 方法
 * @param uri                 请求路径
 * @param queryString         查询串（可能为空）
 * @param clientIp            客户端 IP
 * @param userAgent           User-Agent（可能为空）
 * @param username            当前登录用户名，未认证为 null
 * @param status              响应状态码
 * @param elapsedMs           接口耗时（毫秒）
 * @param requestContentType  请求 Content-Type（可能为空）
 * @param requestBody         请求报文字节
 * @param responseContentType 响应 Content-Type（可能为空）
 * @param responseBody        响应报文字节
 */
public record AccessLogRecord(
        String traceId,
        String method,
        String uri,
        String queryString,
        String clientIp,
        String userAgent,
        String username,
        int status,
        long elapsedMs,
        String requestContentType,
        byte[] requestBody,
        String responseContentType,
        byte[] responseBody
) {
}
