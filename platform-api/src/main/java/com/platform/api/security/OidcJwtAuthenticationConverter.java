package com.platform.api.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.util.ArrayList;
import java.util.List;

/**
 * 将统一认证中心（platform-rs-auth）签发的 JWT 转换为本系统的 LoginUser：
 * 认证（你是谁）来自认证中心，授权（你能做什么）仍按本系统数据库实时加载。
 */
public class OidcJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final OAuth2Error INVALID_TOKEN = new OAuth2Error("invalid_token");

    private final UserAuthorityService userAuthorityService;
    private final String expectedIssuer;

    public OidcJwtAuthenticationConverter(UserAuthorityService userAuthorityService,
                                          String expectedIssuer) {
        this.userAuthorityService = userAuthorityService;
        this.expectedIssuer = expectedIssuer;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        // 校验令牌签发者
        if (expectedIssuer != null && !expectedIssuer.isBlank()
                && !expectedIssuer.equals(String.valueOf(jwt.getIssuer()))) {
            throw new JwtValidationException("令牌签发者不受信任", List.of(INVALID_TOKEN));
        }

        // 认证中心在令牌中写入 uid（用户 ID）声明
        String uidClaim = jwt.getClaimAsString("uid");
        Long userId;
        try {
            userId = Long.valueOf(uidClaim);
        } catch (NumberFormatException ex) {
            throw new JwtValidationException("令牌缺少用户标识", List.of(INVALID_TOKEN));
        }

        LoginUser loginUser = userAuthorityService.load(userId);
        if (loginUser == null) {
            throw new JwtValidationException("用户不存在或已禁用", List.of(INVALID_TOKEN));
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        loginUser.roles().forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        loginUser.perms().forEach(perm -> authorities.add(new SimpleGrantedAuthority(perm)));

        return new UsernamePasswordAuthenticationToken(loginUser, jwt.getTokenValue(), authorities);
    }
}
