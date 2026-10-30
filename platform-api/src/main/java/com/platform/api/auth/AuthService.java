package com.platform.api.auth;

import com.platform.api.common.BusinessException;
import com.platform.api.system.user.SysUser;
import com.platform.api.system.user.SysUserRepository;
import com.platform.api.security.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final SysUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CaptchaService captchaService;

    public AuthService(SysUserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       CaptchaService captchaService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.captchaService = captchaService;
    }

    public AuthDtos.LoginResult login(AuthDtos.LoginRequest request) {
        if (request.username() == null || request.username().isBlank() || request.password() == null) {
            throw BusinessException.badRequest("用户名和密码不能为空");
        }
        captchaService.validate(request.captchaId(), request.captchaCode());

        SysUser user = userRepository.findByUsername(request.username().trim())
                .orElseThrow(() -> BusinessException.badRequest("用户名或密码错误"));
        ensureEnabled(user);
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw BusinessException.badRequest("用户名或密码错误");
        }
        return issueTokens(user);
    }

    public AuthDtos.LoginResult refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw BusinessException.unauthorized("刷新令牌无效或已过期");
        }

        Claims claims;
        try {
            claims = jwtService.parse(refreshToken);
        } catch (Exception ex) {
            throw BusinessException.unauthorized("刷新令牌无效或已过期");
        }
        if (!JwtService.TYPE_REFRESH.equals(claims.get(JwtService.CLAIM_TYPE, String.class))) {
            throw BusinessException.unauthorized("刷新令牌无效或已过期");
        }

        Long userId;
        try {
            userId = Long.valueOf(claims.getSubject());
        } catch (NumberFormatException ex) {
            throw BusinessException.unauthorized("刷新令牌无效或已过期");
        }

        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.unauthorized("用户不存在"));
        ensureEnabled(user);
        return issueTokens(user);
    }

    private void ensureEnabled(SysUser user) {
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw BusinessException.badRequest("账号已被禁用，请联系管理员");
        }
    }

    private AuthDtos.LoginResult issueTokens(SysUser user) {
        String accessToken = jwtService.createAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtService.createRefreshToken(user.getId(), user.getUsername());
        return new AuthDtos.LoginResult(accessToken, refreshToken, "Bearer", jwtService.getAccessExpireSeconds());
    }
}
