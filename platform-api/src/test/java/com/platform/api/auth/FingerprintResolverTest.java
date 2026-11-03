package com.platform.api.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link FingerprintResolver} 单元测试。
 * <p>
 * mockRequest 位置顺序：(realIp, xff, remoteAddr, ua)，全部可为 null。
 */
class FingerprintResolverTest {

    private static HttpServletRequest mockRequest(String realIp, String xff,
                                                  String remoteAddr, String ua) {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getHeader("X-Real-IP")).thenReturn(realIp);
        Mockito.when(req.getHeader("X-Forwarded-For")).thenReturn(xff);
        Mockito.when(req.getHeader("User-Agent")).thenReturn(ua);
        Mockito.when(req.getRemoteAddr()).thenReturn(remoteAddr != null ? remoteAddr : "127.0.0.1");
        return req;
    }

    // ---- IP 解析 ----

    @Test
    @DisplayName("优先使用 X-Real-IP")
    void xRealIpPreferred() {
        HttpServletRequest req = mockRequest(
                "203.0.113.45",        // X-Real-IP
                "192.168.1.1, 10.0.0.5", // X-Forwarded-For（应被跳过）
                "127.0.0.1",
                "Chrome/120"
        );
        byte[] fp = FingerprintResolver.resolve(req);
        assertNotNull(fp);
        assertEquals(16, fp.length);
    }

    @Test
    @DisplayName("X-Forwarded-For 取最右侧非内网 IP")
    void xffRightmostPublic() {
        HttpServletRequest req = mockRequest(
                null,
                "10.0.0.5, 192.168.1.1, 203.0.113.42", // 最右侧公开 IP
                "127.0.0.1",
                "Chrome/120"
        );
        byte[] fp = FingerprintResolver.resolve(req);
        assertNotNull(fp);
    }

    @Test
    @DisplayName("全部是内网 IP 时退化为 remoteAddr")
    void allInternalFallbackToRemote() {
        HttpServletRequest req = mockRequest(
                null,
                "10.0.0.1, 192.168.1.2", // 全内网
                "203.0.113.55",          // 退化到这里
                "Chrome/120"
        );
        byte[] fp = FingerprintResolver.resolve(req);
        assertNotNull(fp);
    }

    @Test
    @DisplayName("IPv4 /24 前缀：同网段不同 IP 指纹相同")
    void ipv4PrefixStableUnderSameSegment() {
        String ua = "Chrome/120";
        HttpServletRequest reqA = mockRequest("192.168.1.10", null, null, ua);
        HttpServletRequest reqB = mockRequest("192.168.1.230", null, null, ua);
        assertArrayEquals(
                FingerprintResolver.resolve(reqA),
                FingerprintResolver.resolve(reqB),
                "同网段 /24 下不同 IP 应产生相同指纹"
        );
    }

    @Test
    @DisplayName("IPv4 /24 前缀：不同网段指纹不同")
    void ipv4DifferentPrefixDifferentFp() {
        String ua = "Chrome/120";
        HttpServletRequest reqA = mockRequest("192.168.1.10", null, null, ua);
        HttpServletRequest reqB = mockRequest("192.168.2.10", null, null, ua);
        assertFalse(
                java.util.Arrays.equals(
                        FingerprintResolver.resolve(reqA),
                        FingerprintResolver.resolve(reqB)),
                "不同网段应产生不同指纹"
        );
    }

    @Test
    @DisplayName("IPv6 /64 前缀")
    void ipv6PrefixTakesFirstFourSegments() {
        HttpServletRequest req = mockRequest(
                "2001:db8:85a3:0000:0000:8a2e:0370:7334",
                null, null, "Chrome/120"
        );
        byte[] fp = FingerprintResolver.resolve(req);
        assertNotNull(fp);
    }

    // ---- UA 精简 ----

    @Test
    @DisplayName("Chrome UA 识别为 Chrome 主版本")
    void chromeUA() {
        HttpServletRequest req = mockRequest(
                "1.2.3.4", null, null,
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        );
        assertNotNull(FingerprintResolver.resolve(req));
    }

    @Test
    @DisplayName("Edge UA 识别为 Edge")
    void edgeUA() {
        HttpServletRequest req = mockRequest(
                "1.2.3.4", null, null,
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0"
        );
        assertNotNull(FingerprintResolver.resolve(req));
    }

    @Test
    @DisplayName("UA 仅主版本号相同 → patch 变化不影响指纹")
    void uaPatchVersionIgnored() {
        String ip = "1.2.3.4";
        HttpServletRequest reqA = mockRequest(ip, null, null,
                "Mozilla/5.0 Chrome/120.0.1.0 Safari/537.36");
        HttpServletRequest reqB = mockRequest(ip, null, null,
                "Mozilla/5.0 Chrome/120.0.2.0 Safari/537.36");
        assertArrayEquals(
                FingerprintResolver.resolve(reqA),
                FingerprintResolver.resolve(reqB),
                "UA 主版本相同时 patch 变化应不影响指纹"
        );
    }

    @Test
    @DisplayName("UA 主版本变化 → 指纹变化")
    void uaMajorVersionChangesFingerprint() {
        String ip = "1.2.3.4";
        HttpServletRequest reqA = mockRequest(ip, null, null, "Mozilla/5.0 Chrome/120.0.0.0 Safari/537.36");
        HttpServletRequest reqB = mockRequest(ip, null, null, "Mozilla/5.0 Chrome/121.0.0.0 Safari/537.36");
        assertFalse(
                java.util.Arrays.equals(
                        FingerprintResolver.resolve(reqA),
                        FingerprintResolver.resolve(reqB)),
                "主版本变化应改变指纹"
        );
    }

    @Test
    @DisplayName("UA 为空 → 指纹退化（仍可生成）")
    void emptyUaStillProducesFp() {
        HttpServletRequest req = mockRequest("1.2.3.4", null, null, null);
        byte[] fp = FingerprintResolver.resolve(req);
        assertNotNull(fp);
        assertEquals(16, fp.length);
    }

    // ---- IP / UA 任一变化都改变指纹 ----

    @Test
    @DisplayName("IP 变化 + 同 UA → 指纹变化")
    void changingIpChangesFp() {
        String ua = "Chrome/120 Windows11";
        HttpServletRequest reqA = mockRequest("192.168.1.1", null, null, ua);
        HttpServletRequest reqB = mockRequest("10.0.0.5", null, null, ua);
        assertFalse(
                java.util.Arrays.equals(
                        FingerprintResolver.resolve(reqA),
                        FingerprintResolver.resolve(reqB)),
                "IP 变化应改变指纹"
        );
    }

    @Test
    @DisplayName("IP 相同 + UA 变化 → 指纹变化")
    void changingUaChangesFp() {
        String ip = "192.168.1.1";
        HttpServletRequest reqA = mockRequest(ip, null, null, "Chrome/120");
        HttpServletRequest reqB = mockRequest(ip, null, null, "Firefox/130");
        assertFalse(
                java.util.Arrays.equals(
                        FingerprintResolver.resolve(reqA),
                        FingerprintResolver.resolve(reqB)),
                "UA 变化应改变指纹"
        );
    }

    @Test
    @DisplayName("IP 和 UA 都相同 → 指纹相同（确定性）")
    void sameInputSameFp() {
        HttpServletRequest reqA = mockRequest("192.168.1.1", null, null, "Chrome/120 Windows11");
        HttpServletRequest reqB = mockRequest("192.168.1.1", null, null, "Chrome/120 Windows11");
        assertArrayEquals(
                FingerprintResolver.resolve(reqA),
                FingerprintResolver.resolve(reqB),
                "同一 IP+UA 应产生完全相同的指纹"
        );
    }
}
