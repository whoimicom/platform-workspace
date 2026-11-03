package com.platform.api.auth;

import jakarta.servlet.http.HttpServletRequest;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 请求指纹解析器。
 * <p>
 * 指纹 = SHA-256({@code canonicalIpPrefix || uaIdentity})，取前 16 字节。
 * <ul>
 *   <li>IP 优先取 {@code X-Real-IP} → {@code X-Forwarded-For} 最右侧非内网 IP → remoteAddr；
 *       IPv4 取 /24 前缀（前三段），IPv6 取 /64 前缀，避免 NAT 下误杀。</li>
 *   <li>UA 仅保留浏览器名/主版本 + OS/主版本，忽略窗口大小、DPR 等易变细节。</li>
 * </ul>
 */
public final class FingerprintResolver {

    private FingerprintResolver() {
    }

    /**
     * 从 HttpServletRequest 解析指纹。
     */
    public static byte[] resolve(HttpServletRequest request) {
        String ipPrefix = canonicalIpPrefix(request);
        String uaIdentity = uaIdentity(request.getHeader("User-Agent"));
        return sha256First16(ipPrefix + "|" + uaIdentity);
    }

    /**
     * 从 HttpServletRequest 提取 canonical IP（优先 X-Real-IP → X-Forwarded-For 最右侧公开 IP → remoteAddr），
     * 返回原始 IP（非前缀裁剪），用于 per-IP 防刷等场景。
     */
    public static String resolveIp(HttpServletRequest request) {
        String raw = request.getHeader("X-Real-IP");
        if (isBlank(raw)) {
            raw = rightmostPublicIp(request.getHeader("X-Forwarded-For"));
        }
        if (isBlank(raw)) {
            raw = request.getRemoteAddr();
        }
        return raw == null ? "unknown" : raw;
    }

    // ---- IP 前缀 ----

    private static String canonicalIpPrefix(HttpServletRequest request) {
        return prefix(resolveIp(request));
    }

    /**
     * X-Forwarded-For 取最右侧一个非内网 IP。
     */
    private static String rightmostPublicIp(String xff) {
        if (isBlank(xff)) {
            return null;
        }
        String[] chain = xff.split(",");
        for (int i = chain.length - 1; i >= 0; i--) {
            String candidate = chain[i].trim();
            if (!isBlank(candidate) && !isInternalIp(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean isInternalIp(String ip) {
        try {
            InetAddress addr = InetAddress.getByName(ip.split(":")[0].split("%")[0]);
            return addr.isLoopbackAddress()
                    || addr.isSiteLocalAddress()
                    || addr.isLinkLocalAddress();
        } catch (UnknownHostException ex) {
            return true;
        }
    }

    /**
     * IP 前缀：IPv4 /24，IPv6 /64。
     */
    private static String prefix(String ip) {
        if (isBlank(ip)) {
            return "";
        }
        // IPv6
        if (ip.contains(":")) {
            int zone = ip.indexOf('%');
            if (zone >= 0) {
                ip = ip.substring(0, zone);
            }
            String[] parts = ip.split(":");
            StringBuilder sb = new StringBuilder();
            int limit = Math.min(parts.length, 4); // 每段 16 位，4 段 = 64 位
            for (int i = 0; i < limit; i++) {
                if (i > 0) sb.append(':');
                sb.append(parts[i].isEmpty() ? "0" : parts[i]);
            }
            return sb.toString();
        }
        // IPv4 → /24
        String[] parts = ip.split("\\.");
        if (parts.length >= 3) {
            return parts[0] + "." + parts[1] + "." + parts[2];
        }
        return ip;
    }

    // ---- UA 精简 ----

    /**
     * 精简 UA，只保留浏览器/版本 + OS/版本。
     * 规则从粗到细尝试匹配，最后保底返回前 40 字符（避免完全丢失）。
     */
    private static String uaIdentity(String ua) {
        if (isBlank(ua)) {
            return "unknown";
        }

        // 浏览器
        String browser = "Other";
        String bVersion = "";
        String lower = ua.toLowerCase();

        if (lower.contains("edg/")) {
            browser = "Edge";
            bVersion = extractVersion(ua, "Edg/");
        } else if (lower.contains("chrome/") && !lower.contains("edg/") && !lower.contains("opr/") && !lower.contains("crios/")) {
            browser = "Chrome";
            bVersion = extractVersion(ua, "Chrome/");
        } else if (lower.contains("opr/") || lower.contains("opera/")) {
            browser = "Opera";
            bVersion = extractVersion(ua, "OPR/");
        } else if (lower.contains("crios/")) {
            browser = "Chrome-iOS";
            bVersion = extractVersion(ua, "CriOS/");
        } else if (lower.contains("safari/") && lower.contains("version/")) {
            browser = "Safari";
            bVersion = extractVersion(ua, "Version/");
        } else if (lower.contains("firefox/")) {
            browser = "Firefox";
            bVersion = extractVersion(ua, "Firefox/");
        }

        // OS
        String os = "Unknown";
        if (lower.contains("windows nt 11")) {
            os = "Windows11";
        } else if (lower.contains("windows nt 10")) {
            os = "Windows10";
        } else if (lower.contains("windows nt 6.3")) {
            os = "Windows8";
        } else if (lower.contains("mac os x 15")) {
            os = "macOS15";
        } else if (lower.contains("mac os x 14")) {
            os = "macOS14";
        } else if (lower.contains("mac os x 13")) {
            os = "macOS13";
        } else if (lower.contains("mac os x")) {
            os = "macOS";
        } else if (lower.contains("android")) {
            os = "Android";
        } else if (lower.contains("iphone") || lower.contains("ipad")) {
            os = "iOS";
        } else if (lower.contains("linux")) {
            os = "Linux";
        }

        if (bVersion.length() > 0) {
            return browser + "/" + bVersion + " " + os;
        }
        return browser + " " + os;
    }

    private static String extractVersion(String ua, String marker) {
        int idx = ua.indexOf(marker);
        if (idx < 0) {
            return "";
        }
        int start = idx + marker.length();
        int end = start;
        while (end < ua.length() && (Character.isDigit(ua.charAt(end)) || ua.charAt(end) == '.')) {
            end++;
        }
        String v = ua.substring(start, end);
        // 只保留主版本号（第一个点之前）
        int dot = v.indexOf('.');
        if (dot > 0) {
            v = v.substring(0, dot);
        }
        return v;
    }

    // ---- SHA-256 ----

    private static byte[] sha256First16(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] full = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[16];
            System.arraycopy(full, 0, out, 0, 16);
            return out;
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 不可用", ex);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
