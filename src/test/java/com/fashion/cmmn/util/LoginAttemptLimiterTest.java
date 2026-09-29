package com.fashion.cmmn.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptLimiterTest {

    private final long[] now = {1_000_000L};
    private LoginAttemptLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new LoginAttemptLimiter(() -> now[0], 100);
    }

    private int acquire(LoginAttemptLimiter target, String key, int times) {
        int allowed = 0;
        for (int i = 0; i < times; i++) {
            if (target.tryAcquire(key)) {
                allowed++;
            }
        }
        return allowed;
    }

    @Test
    void firstFiveAttemptsAllowed() {
        assertEquals(5, acquire(limiter, "a", 5));
    }

    @Test
    void sixthAttemptBlocked() {
        acquire(limiter, "a", 5);
        assertFalse(limiter.tryAcquire("a"));
    }

    @Test
    void blockExpiresAfterFiveMinutes() {
        acquire(limiter, "a", 5);
        now[0] += LoginAttemptLimiter.BLOCK_MILLIS - 1;
        assertFalse(limiter.tryAcquire("a"));
        now[0] += 1;
        assertEquals(5, acquire(limiter, "a", 5)); // 차단이 끝나면 횟수도 새로 센다
        assertFalse(limiter.tryAcquire("a"));
    }

    @Test
    void oldAttemptsForgotten() {
        acquire(limiter, "a", 4);
        now[0] += LoginAttemptLimiter.BLOCK_MILLIS + 1;
        assertEquals(5, acquire(limiter, "a", 5));
    }

    @Test
    void resetClears() {
        acquire(limiter, "a", 4);
        limiter.reset("a");
        assertEquals(5, acquire(limiter, "a", 5));
    }

    @Test
    void keysAreIndependent() {
        acquire(limiter, "a", 6);
        assertTrue(limiter.tryAcquire("b"));
    }

    // 동시에 몰려와도 확인과 증가가 한 번에 일어나 5번까지만 통과한다
    @Test
    void concurrentAttemptsCannotExceedLimit() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger allowed = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                if (limiter.tryAcquire("a")) {
                    allowed.incrementAndGet();
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();
        assertEquals(5, allowed.get());
    }

    // 키가 상한만큼 차 있으면 새 키는 막는다(메모리 소모 공격 방지). 이미 있는 키는 계속 센다
    @Test
    void newKeysRejectedWhenFull() {
        LoginAttemptLimiter small = new LoginAttemptLimiter(() -> now[0], 3);
        small.tryAcquire("a");
        small.tryAcquire("b");
        small.tryAcquire("c");
        assertFalse(small.tryAcquire("d"));
        assertTrue(small.tryAcquire("a"));
    }

    @Test
    void staleKeysPurgedWhenFull() {
        LoginAttemptLimiter small = new LoginAttemptLimiter(() -> now[0], 3);
        small.tryAcquire("a");
        small.tryAcquire("b");
        small.tryAcquire("c");
        now[0] += LoginAttemptLimiter.BLOCK_MILLIS + 1;
        assertTrue(small.tryAcquire("d"));
    }
}
