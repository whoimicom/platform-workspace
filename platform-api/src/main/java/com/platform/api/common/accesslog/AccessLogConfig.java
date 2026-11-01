package com.platform.api.common.accesslog;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 接口访问日志装配：过滤器（全链路最早）+ 拦截器（注解判定与用户名采集）。
 */
@Configuration
@EnableConfigurationProperties(AccessLogProperties.class)
public class AccessLogConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AccessLogInterceptor()).addPathPatterns("/api/**");
    }

    @Bean
    public FilterRegistrationBean<AccessLogFilter> accessLogFilter(
            AccessLogProperties properties, AccessLogAsyncLogger asyncLogger) {
        FilterRegistrationBean<AccessLogFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AccessLogFilter(properties, asyncLogger));
        registration.addUrlPatterns("/*");
        // 先于 Spring Security 过滤器链执行，保证最外层统计与报文缓存
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.setName("accessLogFilter");
        return registration;
    }
}
