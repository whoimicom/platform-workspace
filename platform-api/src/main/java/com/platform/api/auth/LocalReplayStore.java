package com.platform.api.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Caffeine 的本地防重放黑名单。
 * <p>
 * Key 为 captchaToken 本身（已通过签名校验，不可伪造），TTL 由写入时显式指定，
 * 通常等于 token 剩余的有效期（秒）。
 * <p>
 * 集群下每台机器独立，因此最多可以绕过 {@code 节点数} 次（方案 A 已知约束）。
 * 本组件为可替换抽象，后续若引入 Redis SETNX 可保持接口不变。
 */
public final class LocalReplayStore {

    private final Cache<String, Entry> cache;

    public LocalReplayStore(int maxSize) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfter(new Expiry<String, Entry>() {
                    @Override
                    public long expireAfterCreate(String key, Entry value, long currentTime) {
                        return Duration.ofSeconds(value.ttlSeconds).toNanos();
                    }

                    @Override
                    public long expireAfterUpdate(String key, Entry value, long currentTime, long currentDuration) {
                        // 更新（重复写入）仍以 value.ttlSeconds 为准
                        return Duration.ofSeconds(value.ttlSeconds).toNanos();
                    }

                    @Override
                    public long expireAfterRead(String key, Entry value, long currentTime, long currentDuration) {
                        // 读操作不续期
                        return currentDuration;
                    }
                })
                .build();
    }

    /**
     * 尝试消费 token；仅第一次调用返回 true。
     *
     * @param token      captchaToken
     * @param ttlSeconds 该条目的有效期（秒），通常等于 token 剩余有效期
     * @return {@code true} 表示首次消费成功；{@code false} 表示已被消费过（重放）
     */
    public boolean tryConsume(String token, long ttlSeconds) {
        Entry previous = cache.asMap().putIfAbsent(token, Entry.of(ttlSeconds));
        return previous == null;
    }

    /** 调试用：是否已消费 */
    public boolean hasConsumed(String token) {
        return cache.getIfPresent(token) != null;
    }

    private record Entry(long ttlSeconds) {
        static Entry of(long ttl) {
            return new Entry(Math.max(1L, ttl));
        }
    }
}
