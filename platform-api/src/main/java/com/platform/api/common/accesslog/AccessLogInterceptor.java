package com.platform.api.common.accesslog;

import com.platform.api.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 在请求线程内补充两类信息：
 * <ul>
 *     <li>{@link IgnoreAccessLog} 注解判定，命中则标记跳过日志</li>
 *     <li>当前登录用户名（过滤器运行在 Spring Security 之前取不到认证信息，
 *     拦截器位于 DispatcherServlet 内，此时安全上下文已就绪）</li>
 * </ul>
 */
public class AccessLogInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        if (handler instanceof HandlerMethod handlerMethod) {
            if (isIgnored(handlerMethod)) {
                request.setAttribute(AccessLogFilter.SKIP_ATTRIBUTE, Boolean.TRUE);
            }
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                String username = switch (principal) {
                    case LoginUser loginUser -> loginUser.username();
                    case UserDetails userDetails -> userDetails.getUsername();
                    case null, default -> String.valueOf(principal);
                };
                if (!"anonymousUser".equals(username)) {
                    request.setAttribute(AccessLogFilter.USERNAME_ATTRIBUTE, username);
                }
            }
        }
        return true;
    }

    private boolean isIgnored(HandlerMethod handlerMethod) {
        if (handlerMethod.getMethodAnnotation(IgnoreAccessLog.class) != null) {
            return true;
        }
        return handlerMethod.getBeanType().getAnnotation(IgnoreAccessLog.class) != null;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        // 日志输出由过滤器统一完成
    }
}
