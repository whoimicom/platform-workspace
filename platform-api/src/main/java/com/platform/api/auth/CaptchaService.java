package com.platform.api.auth;

import com.platform.api.common.BusinessException;
import com.platform.api.config.AppProperties;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

/**
 * 图形验证码服务。
 * <p>
 * 采用「签名 Token + 请求指纹 + 本地 TTL 防重放」模式，无 Redis 依赖即可支持集群：
 * <ol>
 *   <li>生成：内存生成 code → {@link CaptchaSigner} 生成签名 token 回显给前端；</li>
 *   <li>校验：任意集群节点可通过 HMAC 验签 + 指纹比对 + 本地 Caffeine 防重放完成。</li>
 * </ol>
 * 已知约束：集群 N 节点下最多可绕过 N 次（每台机器黑名单独立），属于方案 A 的接受上限。
 */
@Service
public class CaptchaService {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 44;

    private final AppProperties properties;
    private final SecureRandom random = new SecureRandom();

    private CaptchaSigner signer;
    private LocalReplayStore replayStore;
    private Duration expireDuration;

    public CaptchaService(AppProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        AppProperties.Captcha captcha = properties.getCaptcha();
        String secret = captcha.getSecret();
        if (secret == null || secret.isBlank()) {
            secret = properties.getJwt().getSecret();
        }
        this.signer = new CaptchaSigner(secret);
        this.replayStore = new LocalReplayStore(captcha.getReplayCacheMaxSize());
        this.expireDuration = Duration.ofSeconds(captcha.getExpireSeconds());
    }

    /**
     * 生成验证码。
     *
     * @param request 当前 HttpServletRequest（用于生成指纹，使 token 与请求方绑定）
     */
    public AuthDtos.CaptchaInfo create(HttpServletRequest request) {
        int left = random.nextInt(9) + 1;
        int right = random.nextInt(9) + 1;
        String code = String.valueOf(left + right);
        String text = left + "+" + right + "=?";

        byte[] fingerprint = FingerprintResolver.resolve(request);
        String captchaToken = signer.sign(code, fingerprint, expireDuration);

        return new AuthDtos.CaptchaInfo(captchaToken, render(text));
    }

    /**
     * 校验验证码。通过后立即在本地 Caffeine 黑名单登记，防止同一 token 重复消费。
     *
     * @param captchaToken 用户提交的签名 token
     * @param captchaCode  用户提交的验证码明文
     * @param request      当前请求（用于比对指纹）
     */
    public void validate(String captchaToken, String captchaCode, HttpServletRequest request) {
        if (!properties.isCaptchaEnabled()) {
            return;
        }
        if (captchaToken == null || captchaCode == null) {
            throw BusinessException.badRequest("验证码缺失，请刷新后重试");
        }

        byte[] fingerprint = FingerprintResolver.resolve(request);
        long now = System.currentTimeMillis() / 1000L;

        // 1) 完整校验（过期 + 指纹 + HMAC）
        try {
            signer.verify(captchaToken, captchaCode.trim(), fingerprint, expireDuration, now);
        } catch (IllegalArgumentException ex) {
            throw BusinessException.badRequest(ex.getMessage());
        }

        // 2) 防重放：本地 Caffeine 黑名单，TTL = token 剩余有效期
        try {
            CaptchaSigner.ParseResult parsed = signer.parse(captchaToken);
            long ttl = parsed.timestamp() + expireDuration.toSeconds() - now;
            if (!replayStore.tryConsume(captchaToken, ttl)) {
                throw BusinessException.badRequest("验证码已使用，请刷新后重试");
            }
        } catch (IllegalArgumentException ex) {
            // parse 失败不会发生（verify 已成功），兜底处理
            throw BusinessException.badRequest("验证码格式非法，请刷新后重试");
        }
    }

    private String render(String text) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(244, 247, 252));
            graphics.fillRect(0, 0, WIDTH, HEIGHT);

            for (int i = 0; i < 4; i++) {
                graphics.setColor(new Color(random.nextInt(160) + 60,
                        random.nextInt(160) + 60, random.nextInt(160) + 60));
                graphics.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                        random.nextInt(WIDTH), random.nextInt(HEIGHT));
            }

            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
            graphics.setColor(new Color(37, 99, 235));
            graphics.drawString(text, 18, 32);
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException ex) {
            throw new IllegalStateException("验证码图片生成失败", ex);
        }
    }
}
