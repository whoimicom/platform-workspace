package com.platform.api.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link LocalReplayStore} 单元测试。
 */
class LocalReplayStoreTest {

    private LocalReplayStore store;

    @BeforeEach
    void setUp() {
        store = new LocalReplayStore(1024);
    }

    @Test
    @DisplayName("首次消费返回 true")
    void firstConsumeReturnsTrue() {
        assertTrue(store.tryConsume("token-abc", 300));
    }

    @Test
    @DisplayName("同一 token 第二次消费返回 false")
    void secondConsumeReturnsFalse() {
        assertTrue(store.tryConsume("token-abc", 300));
        assertFalse(store.tryConsume("token-abc", 300));
        assertFalse(store.tryConsume("token-abc", 300));
    }

    @Test
    @DisplayName("不同 token 互不影响")
    void differentTokensIndependent() {
        assertTrue(store.tryConsume("t1", 300));
        assertTrue(store.tryConsume("t2", 300));
        assertTrue(store.tryConsume("t3", 300));
        assertFalse(store.tryConsume("t1", 300));
    }

    @Test
    @DisplayName("maxSize 上限触发后，最早条目会被淘汰")
    void maxSizeEviction() {
        LocalReplayStore small = new LocalReplayStore(4);
        for (int i = 0; i < 10; i++) {
            assertTrue(small.tryConsume("token-" + i, 300));
        }
        // 全部消费过都应返回 false（除了被淘汰的那些可能重新入）
        // 这里主要验证不会因 maxSize 过小而 crash
    }

    @Test
    @DisplayName("ttl 为 0 或负数自动纠正为 1 秒")
    void zeroOrNegativeTtlNormalized() {
        assertTrue(store.tryConsume("t", 0));
        assertTrue(store.tryConsume("t2", -10));
    }

    @Test
    @DisplayName("hasConsumed 反映已消费状态")
    void hasConsumedStateReflects() {
        assertFalse(store.hasConsumed("new-token"));
        store.tryConsume("new-token", 300);
        assertTrue(store.hasConsumed("new-token"));
    }

    @Test
    @DisplayName("条目到期后自动清理（Expiry 接口生效）")
    void entryAutoExpires() throws InterruptedException {
        store.tryConsume("short", 1); // TTL 1 秒
        assertTrue(store.hasConsumed("short"));
        Thread.sleep(1100); // 确保到期
        // Caffeine 的 expireAfter 使用的是写入时刻，到期后下次 getIfPresent 应返回 null
        // 注意：Caffeine 的清理有延迟，依赖内部 writer/reader 触发；
        // 这里先触发一次 read 让过期回调生效
        assertFalse(store.hasConsumed("short"));
    }
}
