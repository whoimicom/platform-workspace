package com.platform.api.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.platform.api.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 短信验证码服务。
 * <p>
 * 当前为 Mock 模式：生成后直接打印到应用控制台，真实发送通道接入后替换 {@link #doSend(String, String)} 即可。
 * <p>
 * 防机器刷两层：
 * <ul>
 *   <li>per-mobile 60s 限 1 次（同一手机号不能频繁发送）</li>
 *   <li>per-IP 60s 限 5 次（防止脚本批量刷不同手机号）</li>
 * </ul>
 */
@Service
public class SmsCodeService {

    private static final Logger log = LoggerFactory.getLogger(SmsCodeService.class);

    /** 验证码有效期（秒） */
    private static final long CODE_TTL_SECONDS = 300;

    /** per-mobile 发送间隔（秒） */
    private static final long MOBILE_COOLDOWN_SECONDS = 60;

    /** per-IP 窗口内最大发送次数 */
    private static final int IP_MAX_PER_MINUTE = 5;

    /** 验证码存的是以 "mobile" 为 key，值为 code（纯数字字符串） */
    private final Cache<String, String> codeCache;

    /** per-mobile 发送节流，TTL = MOBILE_COOLDOWN_SECONDS */
    private final Cache<String, Boolean> mobileThrottleCache;

    /** per-IP 计数，TTL 60s，value = 当前分钟已发送次数 */
    private final Cache<String, Integer> ipCounterCache;

    private final SecureRandom random = new SecureRandom();

    /** 图形验证码服务：发送短信前必须通过图形验证，防止脚本直接刷短信 */
    private final CaptchaService captchaService;

    public SmsCodeService(CaptchaService captchaService) {
        this.captchaService = captchaService;
        this.codeCache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfter(new FixedTtlExpiry<>(CODE_TTL_SECONDS))
                .build();

        this.mobileThrottleCache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfter(new FixedTtlExpiry<>(MOBILE_COOLDOWN_SECONDS))
                .build();

        this.ipCounterCache = Caffeine.newBuilder()
                .maximumSize(2_000)
                .expireAfterWrite(Duration.ofSeconds(60))
                .build();
    }

    /**
     * 发送验证码（内部完成校验 + 生成 + mock 发送）。
     *
     * @param mobile       手机号
     * @param request      当前 HttpServletRequest（用于 IP 防刷与图形验证指纹比对）
     * @param captchaToken 图形验证码 token
     * @param captchaCode  图形验证码明文
     */
    public void send(String mobile, HttpServletRequest request, String captchaToken, String captchaCode) {
        // 0) 图形验证码必须先通过（配置关闭时 validate 直接放行）
        captchaService.validate(captchaToken, captchaCode, request);

        if (mobile == null || !mobile.matches("^1[3-9]\\d{9}$")) {
            throw BusinessException.badRequest("手机号格式不正确");
        }

        // 1) per-mobile 节流
        if (mobileThrottleCache.getIfPresent(mobile) != null) {
            throw BusinessException.badRequest("发送过于频繁，请稍后再试");
        }

        // 2) per-IP 节流
        String ip = FingerprintResolver.resolveIp(request);
        int count = ipCounterCache.asMap().merge(ip, 1, Integer::sum);
        if (count > IP_MAX_PER_MINUTE) {
            throw BusinessException.badRequest("当前网络请求过于频繁，请稍后再试");
        }

        // 3) 生成 6 位数字验证码
        String code = generateCode();
        codeCache.put(mobile, code);
        mobileThrottleCache.put(mobile, Boolean.TRUE);

        // 4) mock 发送
        doSend(mobile, code);
    }

    /**
     * 校验验证码。
     *
     * @param mobile 用户提交的手机号
     * @param code   用户提交的验证码
     * @throws BusinessException 过期或不匹配
     */
    public void verify(String mobile, String code) {
        if (mobile == null || code == null) {
            throw BusinessException.badRequest("验证码或手机号缺失");
        }
        String expected = codeCache.getIfPresent(mobile);
        if (expected == null) {
            throw BusinessException.badRequest("验证码已过期，请重新获取");
        }
        if (!expected.equals(code.trim())) {
            throw BusinessException.badRequest("验证码错误");
        }
        // 一次性消费
        codeCache.invalidate(mobile);
    }

    // ---- 内部 ----

    private String generateCode() {
        int num = random.nextInt(1_000_000); // [0, 999999]
        return String.format("%06d", num);
    }

    /**
     * 真实发送通道接入后替换本方法（例如调用阿里云 SMS / 腾讯云 SMS SDK）。
     * 当前直接打印到应用控制台。
     */
    protected void doSend(String mobile, String code) {
        // TODO: 接入真实短信通道
        log.warn("【MOCK】短信验证码发送 → mobile={}, code={}", mobile, code);
    }

    /** 固定 TTL 的 Expiry 实现 */
    private static class FixedTtlExpiry<V> implements Expiry<String, V> {
        private final long ttlSeconds;

        FixedTtlExpiry(long ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }

        @Override
        public long expireAfterCreate(String key, V value, long currentTime) {
            return Duration.ofSeconds(ttlSeconds).toNanos();
        }

        @Override
        public long expireAfterUpdate(String key, V value, long currentTime, long currentDuration) {
            return Duration.ofSeconds(ttlSeconds).toNanos();
        }

        @Override
        public long expireAfterRead(String key, V value, long currentTime, long currentDuration) {
            return currentDuration;
        }
    }
}
