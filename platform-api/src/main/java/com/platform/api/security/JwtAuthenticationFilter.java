package com.platform.api.security;

import io.jsonwebtoken.Claims;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 解析 Authorization 头中的访问令牌，并写入 SecurityContext。
 * 该类不在容器中注册为 Bean，只挂载到 Security 过滤器链，避免被重复执行。
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserAuthorityService userAuthorityService;

    public JwtAuthenticationFilter(JwtService jwtService, UserAuthorityService userAuthorityService) {
        this.jwtService = jwtService;
        this.userAuthorityService = userAuthorityService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            if (!token.isEmpty()) {
                authenticate(request, token);
            }
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String token) {
        try {
            Claims claims = jwtService.parse(token);
            if (!JwtService.TYPE_ACCESS.equals(claims.get(JwtService.CLAIM_TYPE, String.class))) {
                return;
            }
            Long userId = Long.valueOf(claims.getSubject());
            LoginUser loginUser = userAuthorityService.load(userId);
            if (loginUser == null) {
                return;
            }

            List<GrantedAuthority> authorities = new ArrayList<>();
            loginUser.roles().forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
            loginUser.perms().forEach(perm -> authorities.add(new SimpleGrantedAuthority(perm)));

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(loginUser, token, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception ex) {
            // 令牌无效或过期时保持匿名状态，由 EntryPoint 返回 401
            SecurityContextHolder.clearContext();
        }
    }
}
