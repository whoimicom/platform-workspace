package com.platform.api.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link CaptchaSigner} 单元测试。
 * <p>
 * 覆盖：签名生成、完整校验（过期/指纹/HMAC）、伪造/篡改/重放防御、token 格式边界。
 */
class CaptchaSignerTest {

    private static final String SECRET = "this-is-a-test-secret-key-32bytes-min-ok";
    private static final Duration EXPIRE = Duration.ofSeconds(300);

    private CaptchaSigner signer;

    @BeforeEach
    void setUp() {
        signer = new CaptchaSigner(SECRET);
    }

    // ---- 构造器边界 ----

    @Test
    @DisplayName("密钥过短抛异常")
    void shortSecretRejected() {
        assertThrows(IllegalArgumentException.class, () -> new CaptchaSigner("too-short"));
    }

    @Test
    @DisplayName("密钥为 null 抛异常")
    void nullSecretRejected() {
        assertThrows(IllegalArgumentException.class, () -> new CaptchaSigner(null));
    }

    // ---- 正常签名 & 校验 ----

    @Test
    @DisplayName("签名后完整校验通过")
    void signThenVerifyOk() {
        byte[] fp = testFingerprint("192.168.1.100", "Chrome/120 Windows11");
        String token = signer.sign("7", fp, EXPIRE);

        long now = System.currentTimeMillis() / 1000L;
        assertDoesNotThrow(() -> signer.verify(token, "7", fp, EXPIRE, now));
    }

    @Test
    @DisplayName("token 长度固定 64 字节 → URL-safe Base64 后不含 padding '='")
    void tokenLengthAndPadding() {
        byte[] fp = testFingerprint("10.0.0.1", "Firefox/130 Linux");
        String token = signer.sign("42", fp, EXPIRE);
        // 64 字节 URL-safe Base64 无 padding = 86 字符
        assertEquals(86, token.length(), "URL-safe Base64 长度应为 86");
        assertFalse(token.contains("="), "不应出现 = padding");
        assertFalse(token.contains("+"), "不应出现 +");
        assertFalse(token.contains("/"), "不应出现 /");
    }

    @Test
    @DisplayName("同一 code 多次签名产生不同 token（nonce 生效）")
    void nonceMakesEveryTokenUnique() {
        byte[] fp = testFingerprint("192.168.1.1", "Safari/17 macOS14");
        String t1 = signer.sign("8", fp, EXPIRE);
        // 同一毫秒内两次签名，nonce 不同 → token 必然不同
        String t2 = signer.sign("8", fp, EXPIRE);
        assertNotEquals(t1, t2, "相同 code/fp 也应产生不同 token（nonce 随机性）");
    }

    // ---- 过期 ----

    @Test
    @DisplayName("超出 expire 秒后拒绝")
    void expiredTokenRejected() {
        byte[] fp = testFingerprint("1.2.3.4", "Edge/119 Windows10");
        String token = signer.sign("3", fp, EXPIRE);

        long nowFuture = System.currentTimeMillis() / 1000L + 301; // 超出 300s
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> signer.verify(token, "3", fp, EXPIRE, nowFuture));
        assertTrue(ex.getMessage().contains("过期"), "错误信息应包含'过期'");
    }

    @Test
    @DisplayName("临界值：恰好 expire 秒内可通过")
    void atBoundaryStillValid() {
        byte[] fp = testFingerprint("127.0.0.1", "Chrome/120 Windows11");
        String token = signer.sign("5", fp, EXPIRE);

        long nowBoundary = System.currentTimeMillis() / 1000L + 299;
        assertDoesNotThrow(() -> signer.verify(token, "5", fp, EXPIRE, nowBoundary));
    }

    // ---- 指纹 ----

    @Test
    @DisplayName("指纹不匹配时拒绝")
    void fingerprintMismatchRejected() {
        byte[] fpA = testFingerprint("192.168.1.10", "Chrome/120 Windows11");
        byte[] fpB = testFingerprint("10.0.0.5", "Firefox/130 Linux"); // 不同请求方
        String token = signer.sign("9", fpA, EXPIRE);

        long now = System.currentTimeMillis() / 1000L;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> signer.verify(token, "9", fpB, EXPIRE, now));
        assertTrue(ex.getMessage().contains("不匹配"), "错误信息应包含'不匹配'");
    }

    @Test
    @DisplayName("指纹前 16 字节相同视为匹配（Signer 取前 16 字节）")
    void fingerprint16BytePrefixMatched() {
        byte[] fpA = testFingerprint("192.168.1.1", "Chrome/120 Windows11");
        byte[] fpB = new byte[32];
        System.arraycopy(fpA, 0, fpB, 0, 16); // 只传前 16 字节，其余填 0
        String token = signer.sign("2", fpA, EXPIRE);

        long now = System.currentTimeMillis() / 1000L;
        assertDoesNotThrow(() -> signer.verify(token, "2", fpB, EXPIRE, now));
    }

    @Test
    @DisplayName("指纹为 null 仍能签名（内部归一化）")
    void nullFingerprintSafe() {
        String token = signer.sign("1", null, EXPIRE);
        assertNotNull(token);

        long now = System.currentTimeMillis() / 1000L;
        // 验签时也传 null → 归一化后相同
        assertDoesNotThrow(() -> signer.verify(token, "1", null, EXPIRE, now));
    }

    // ---- HMAC 签名防篡改 ----

    @Test
    @DisplayName("code 错误拒绝")
    void wrongCodeRejected() {
        byte[] fp = testFingerprint("8.8.8.8", "Safari/17 macOS14");
        String token = signer.sign("7", fp, EXPIRE);

        long now = System.currentTimeMillis() / 1000L;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> signer.verify(token, "42", fp, EXPIRE, now));
        assertTrue(ex.getMessage().contains("错误"), "错误信息应包含'错误'");
    }

    @Test
    @DisplayName("大小写不敏感：code 比对区分大小写（签名内容区分，trim 后提交）")
    void codeCaseSensitive() {
        byte[] fp = testFingerprint("1.1.1.1", "Chrome/120 Android");
        String token = signer.sign("AbC", fp, EXPIRE);

        long now = System.currentTimeMillis() / 1000L;
        assertDoesNotThrow(() -> signer.verify(token, "AbC", fp, EXPIRE, now));
        assertThrows(IllegalArgumentException.class,
                () -> signer.verify(token, "abc", fp, EXPIRE, now));
    }

    @Test
    @DisplayName("篡改 token 的 signature 部分后验签失败")
    void tamperedSignatureRejected() {
        byte[] fp = testFingerprint("2.2.2.2", "Chrome/120 iOS");
        String token = signer.sign("5", fp, EXPIRE);

        // 把 token 解码 → 篡改 HMAC 前几个字节 → 再编码
        byte[] raw = java.util.Base64.getUrlDecoder().decode(token);
        raw[61] = (byte) (raw[61] ^ 0xff); // flip 一个 bit
        raw[62] = (byte) (raw[62] ^ 0xff);
        raw[63] = (byte) (raw[63] ^ 0xff);
        String tampered = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        long now = System.currentTimeMillis() / 1000L;
        assertThrows(IllegalArgumentException.class,
                () -> signer.verify(tampered, "5", fp, EXPIRE, now));
    }

    @Test
    @DisplayName("篡改 token 的 timestamp 低字节后验签失败（仍在有效期内）")
    void tamperedTimestampRejected() {
        byte[] fp = testFingerprint("3.3.3.3", "Chrome/120 Windows11");
        String token = signer.sign("6", fp, EXPIRE);

        // 只加一小段时间让 expireAt 仍大于 now，避免被"过期"短路
        byte[] raw = java.util.Base64.getUrlDecoder().decode(token);
        raw[7] = (byte) (raw[7] + 20); // timestamp 最低字节 +20s（仍在 300s 有效期内）
        String tampered = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        long now = System.currentTimeMillis() / 1000L;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> signer.verify(tampered, "6", fp, EXPIRE, now));
        // 非过期错误
        assertFalse(ex.getMessage().contains("过期"), "篡改时间戳后首先应由 HMAC 拒绝，而非过期");
    }

    // ---- token 格式边界 ----

    @Test
    @DisplayName("token 为 null 抛异常")
    void nullTokenRejected() {
        long now = System.currentTimeMillis() / 1000L;
        assertThrows(IllegalArgumentException.class,
                () -> signer.verify(null, "1", new byte[16], EXPIRE, now));
    }

    @Test
    @DisplayName("token 为空字符串抛异常")
    void emptyTokenRejected() {
        long now = System.currentTimeMillis() / 1000L;
        assertThrows(IllegalArgumentException.class,
                () -> signer.verify("", "1", new byte[16], EXPIRE, now));
    }

    @Test
    @DisplayName("token 长度不对抛异常")
    void wrongLengthTokenRejected() {
        long now = System.currentTimeMillis() / 1000L;
        // 随机 100 字节，64 不是其倍数
        String junk = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(new byte[100]);
        assertThrows(IllegalArgumentException.class,
                () -> signer.verify(junk, "1", new byte[16], EXPIRE, now));
    }

    @Test
    @DisplayName("非法 Base64 抛异常")
    void malformedBase64Rejected() {
        long now = System.currentTimeMillis() / 1000L;
        assertThrows(IllegalArgumentException.class,
                () -> signer.verify("!!!not-base64!!!", "1", new byte[16], EXPIRE, now));
    }

    // ---- parse 方法 ----

    @Test
    @DisplayName("parse 能解析 timestamp/nonce/fingerprint")
    void parseExtractsFields() {
        byte[] fp = testFingerprint("10.20.30.40", "Chrome/120 Windows11");
        String token = signer.sign("7", fp, EXPIRE);

        CaptchaSigner.ParseResult result = signer.parse(token);
        assertTrue(result.timestamp() > 0);
        assertEquals(16, result.fingerprint().length);
        assertEquals(32, result.signature().length);
        assertEquals(32, result.body().length); // 8(ts)+8(nonce)+16(fp)
    }

    // ---- 同一 secret 下，不同 code 签名不同（防止同 token 撞出多 code） ----

    @Test
    @DisplayName("同一 token 不能同时校验两个不同 code")
    void oneTokenOneCode() {
        byte[] fp = testFingerprint("5.5.5.5", "Chrome/120 Windows11");
        String token = signer.sign("7", fp, EXPIRE);

        long now = System.currentTimeMillis() / 1000L;
        assertDoesNotThrow(() -> signer.verify(token, "7", fp, EXPIRE, now));
        assertThrows(IllegalArgumentException.class,
                () -> signer.verify(token, "8", fp, EXPIRE, now));
    }

    // ---- 工具 ----

    private static byte[] testFingerprint(String ip, String ua) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] full = digest.digest((ip + "|" + ua).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] out = new byte[16];
            System.arraycopy(full, 0, out, 0, 16);
            return out;
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }
}
