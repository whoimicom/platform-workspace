package com.platform.api.security;

import com.platform.api.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    public static final String CLAIM_TYPE = "type";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final long accessExpireSeconds;
    private final long refreshExpireSeconds;

    public JwtService(AppProperties properties) {
        byte[] secret = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("app.jwt.secret 至少需要 32 个字节");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.accessExpireSeconds = properties.getJwt().getAccessExpireSeconds();
        this.refreshExpireSeconds = properties.getJwt().getRefreshExpireSeconds();
    }

    public String createAccessToken(Long userId, String username) {
        return build(userId, username, TYPE_ACCESS, accessExpireSeconds);
    }

    public String createRefreshToken(Long userId, String username) {
        return build(userId, username, TYPE_REFRESH, refreshExpireSeconds);
    }

    private String build(Long userId, String username, String type, long expireSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim(CLAIM_TYPE, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expireSeconds)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessExpireSeconds() {
        return accessExpireSeconds;
    }
}
