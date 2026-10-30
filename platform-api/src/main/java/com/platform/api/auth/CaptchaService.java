package com.platform.api.auth;

import com.platform.api.common.BusinessException;
import com.platform.api.config.AppProperties;
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
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存版图形验证码。单实例部署足够使用，多实例部署时应替换为 Redis 等共享存储。
 */
@Service
public class CaptchaService {

    private static final long EXPIRE_MILLIS = 5 * 60 * 1000L;
    private static final int WIDTH = 120;
    private static final int HEIGHT = 44;

    private final Map<String, CaptchaEntry> store = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final AppProperties properties;

    public CaptchaService(AppProperties properties) {
        this.properties = properties;
    }

    public AuthDtos.CaptchaInfo create() {
        cleanup();
        int left = random.nextInt(9) + 1;
        int right = random.nextInt(9) + 1;
        String code = String.valueOf(left + right);
        String text = left + "+" + right + "=?";

        String captchaId = UUID.randomUUID().toString().replace("-", "");
        store.put(captchaId, new CaptchaEntry(code, System.currentTimeMillis() + EXPIRE_MILLIS));
        return new AuthDtos.CaptchaInfo(captchaId, render(text));
    }

    /**
     * 校验验证码。验证码一次性使用，无论成功失败都会从内存中移除。
     */
    public void validate(String captchaId, String captchaCode) {
        if (!properties.isCaptchaEnabled()) {
            return;
        }
        CaptchaEntry entry = captchaId == null ? null : store.remove(captchaId);
        if (entry == null || entry.expireAt() < System.currentTimeMillis()) {
            throw BusinessException.badRequest("验证码已过期，请刷新后重试");
        }
        if (captchaCode == null || !entry.code().equalsIgnoreCase(captchaCode.trim())) {
            throw BusinessException.badRequest("验证码错误");
        }
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(item -> item.getValue().expireAt() < now);
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

    private record CaptchaEntry(String code, long expireAt) {
    }
}
