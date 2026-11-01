package com.platform.api.common.accesslog;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记不记录接口访问日志的接口。
 *
 * <p>默认所有 /api/** 接口都会打印请求/返回报文、耗时与 traceId，
 * 个别接口（如内容过大、含敏感信息、心跳类高频接口）可用该注解单独关闭。</p>
 *
 * <p>可标注在 Controller 方法或类上，类上标注对该类所有方法生效，
 * 方法上的标注优先级更高（方法标注后即使类未标注也跳过）。</p>
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface IgnoreAccessLog {
}
