package com.platform.api.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

/**
 * 图形验证码签名工具。
 * <p>
 * Token 结构（共 64 字节，URL-safe Base64 后 ~86 字符）：
 * <pre>
 *   [timestamp:8][nonce:8][fingerprint:16][hmac:32]
 * </pre>
 * 签名覆盖 {@code timestamp || nonce || fingerprint || code}，
 * code 不以明文落盘，仅通过 HMAC 绑定，保证不可伪造、不可重用、不可篡改。
 * <p>
 * 本类不依赖 Spring，可独立复用（短信/滑块验证码均可签名）。
 */
public final class CaptchaSigner {

    private static final String HMAC_ALGO = "HmacSHA256";
    private static final int TS_LEN = Long.BYTES;          // 8
    private static final int NONCE_LEN = 8;
    private static final int FINGERPRINT_LEN = 16;
    private static final int SIGNATURE_LEN = 32;           // HMAC-SHA256 长度
    private static final int TOKEN_LEN = TS_LEN + NONCE_LEN + FINGERPRINT_LEN + SIGNATURE_LEN; // 64

    private final byte[] secret;
    private final SecureRandom random = new SecureRandom();

    public CaptchaSigner(String secret) {
        if (secret == null || secret.length() < 16) {
            throw new IllegalArgumentException("captcha secret 至少需要 16 字节");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    // ---- 签名生成 ----

    /**
     * 对验证码 code 和指纹生成无状态签名 token。
     *
     * @param code        验证码明文
     * @param fingerprint 指纹前缀（通常 SHA-256 前 16 字节）
     * @param expire      有效期
     * @return URL-safe Base64 编码的 token，不可为 null
     */
    public String sign(String code, byte[] fingerprint, Duration expire) {
        long now = System.currentTimeMillis() / 1000L;
        byte[] body = new byte[TS_LEN + NONCE_LEN + FINGERPRINT_LEN];

        // timestamp（大端序）
        writeLong(body, 0, now);
        // nonce（8 字节随机，抵御彩虹表）
        byte[] nonce = new byte[NONCE_LEN];
        random.nextBytes(nonce);
        System.arraycopy(nonce, 0, body, TS_LEN, NONCE_LEN);
        // fingerprint 前缀（截断或扩展到 16 字节）
        byte[] fp16 = normalizeFingerprint(fingerprint);
        System.arraycopy(fp16, 0, body, TS_LEN + NONCE_LEN, FINGERPRINT_LEN);

        byte[] signature = hmac(body, code.getBytes(StandardCharsets.UTF_8));
        byte[] token = new byte[TOKEN_LEN];
        System.arraycopy(body, 0, token, 0, body.length);
        System.arraycopy(signature, 0, token, body.length, SIGNATURE_LEN);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    // ---- 签名校验 ----

    public record ParseResult(
            long timestamp,
            long expireAtSeconds,
            byte[] fingerprint,
            byte[] body,
            byte[] signature
    ) {
    }

    /**
     * 解析 token 但不校验签名。用于上层自行比对指纹、判断过期后再验签。
     *
     * @throws IllegalArgumentException token 非法（格式或长度错误）
     */
    public ParseResult parse(String token) {
        if (token == null) {
            throw new IllegalArgumentException("captchaToken 为空");
        }
        byte[] raw;
        try {
            raw = Base64.getUrlDecoder().decode(token);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("captchaToken 格式非法");
        }
        if (raw.length != TOKEN_LEN) {
            throw new IllegalArgumentException("captchaToken 长度非法");
        }

        long timestamp = readLong(raw, 0);
        byte[] body = new byte[TS_LEN + NONCE_LEN + FINGERPRINT_LEN];
        System.arraycopy(raw, 0, body, 0, body.length);
        byte[] fingerprint = new byte[FINGERPRINT_LEN];
        System.arraycopy(raw, TS_LEN + NONCE_LEN, fingerprint, 0, FINGERPRINT_LEN);
        byte[] signature = new byte[SIGNATURE_LEN];
        System.arraycopy(raw, body.length, signature, 0, SIGNATURE_LEN);

        return new ParseResult(timestamp, 0, fingerprint, body, signature);
    }

    /**
     * 对 token 进行完整校验：过期检查 + 指纹比对 + 签名比对。
     * 指纹与 code 均需匹配，且 timestamp 不得超过 expire。
     *
     * @param token       用户提交的 captchaToken
     * @param code        用户提交的验证码明文
     * @param fingerprint 当前请求指纹（与生成 token 时使用的同一规则）
     * @param expire      有效期（秒）
     * @param nowSeconds  当前时间（秒）
     * @throws IllegalArgumentException 任何一步校验失败
     */
    public void verify(String token,
                       String code,
                       byte[] fingerprint,
                       Duration expire,
                       long nowSeconds) {

        ParseResult parsed = parse(token);

        // 过期校验
        long expireAt = parsed.timestamp() + expire.toSeconds();
        if (nowSeconds >= expireAt) {
            throw new IllegalArgumentException("验证码已过期，请刷新后重试");
        }

        // 指纹校验（常量时间比较）
        byte[] fp16 = normalizeFingerprint(fingerprint);
        if (!constantTimeEquals(fp16, parsed.fingerprint())) {
            throw new IllegalArgumentException("验证码与当前请求不匹配");
        }

        // HMAC 校验
        byte[] expected = hmac(parsed.body(), code.getBytes(StandardCharsets.UTF_8));
        if (!constantTimeEquals(expected, parsed.signature())) {
            throw new IllegalArgumentException("验证码错误");
        }
    }

    // ---- 内部工具 ----

    private byte[] hmac(byte[] body, byte[] extra) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secret, HMAC_ALGO));
            mac.update(body);
            mac.update(extra);
            return mac.doFinal();
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("HMAC-SHA256 不可用", ex);
        }
    }

    private static byte[] normalizeFingerprint(byte[] fp) {
        byte[] out = new byte[FINGERPRINT_LEN];
        if (fp == null) {
            return out;
        }
        int copy = Math.min(fp.length, FINGERPRINT_LEN);
        System.arraycopy(fp, 0, out, 0, copy);
        return out;
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }

    private static void writeLong(byte[] buf, int off, long value) {
        buf[off] = (byte) (value >>> 56);
        buf[off + 1] = (byte) (value >>> 48);
        buf[off + 2] = (byte) (value >>> 40);
        buf[off + 3] = (byte) (value >>> 32);
        buf[off + 4] = (byte) (value >>> 24);
        buf[off + 5] = (byte) (value >>> 16);
        buf[off + 6] = (byte) (value >>> 8);
        buf[off + 7] = (byte) value;
    }

    private static long readLong(byte[] buf, int off) {
        return ((long) buf[off] & 0xFFL) << 56
                | ((long) buf[off + 1] & 0xFFL) << 48
                | ((long) buf[off + 2] & 0xFFL) << 40
                | ((long) buf[off + 3] & 0xFFL) << 32
                | ((long) buf[off + 4] & 0xFFL) << 24
                | ((long) buf[off + 5] & 0xFFL) << 16
                | ((long) buf[off + 6] & 0xFFL) << 8
                | (long) buf[off + 7] & 0xFFL;
    }
}
