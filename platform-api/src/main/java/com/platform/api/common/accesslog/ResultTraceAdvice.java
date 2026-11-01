package com.platform.api.common.accesslog;

import com.platform.api.common.ResultInfo;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 为 {@link ResultInfo} 自动注入扩展字段 traceId / timestamp，
 * 前端可据此反馈排障信息，无需控制器手工设置。
 */
@RestControllerAdvice
public class ResultTraceAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        Class<?> parameterType = returnType.getParameterType();
        if (ResultInfo.class.isAssignableFrom(parameterType)) {
            return true;
        }
        // 兼容异常处理器返回 ResponseEntity<ResultInfo<?>> 的场景
        if (ResponseEntity.class.isAssignableFrom(parameterType)
                && returnType.getGenericParameterType() instanceof ParameterizedType parameterizedType) {
            Type actualType = parameterizedType.getActualTypeArguments()[0];
            Class<?> bodyType = actualType instanceof Class<?> clazz
                    ? clazz
                    : actualType instanceof ParameterizedType nested
                            ? (Class<?>) nested.getRawType()
                            : null;
            return bodyType != null && ResultInfo.class.isAssignableFrom(bodyType);
        }
        return false;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (body instanceof ResultInfo<?> resultInfo) {
            String traceId = MDC.get(AccessLogFilter.TRACE_ID_KEY);
            if (traceId != null) {
                resultInfo.setTraceId(traceId);
            }
            resultInfo.setTimestamp(System.currentTimeMillis());
        }
        return body;
    }
}
