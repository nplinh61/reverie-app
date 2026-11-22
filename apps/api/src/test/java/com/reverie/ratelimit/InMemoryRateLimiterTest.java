package com.reverie.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Unit test for {@link InMemoryRateLimiter}: counting, window reset, and concurrency. */
class InMemoryRateLimiterTest {

    private static final RateLimitPolicy THREE_PER_MINUTE =
            new RateLimitPolicy(3, Duration.ofMinutes(1));

    @Test
    void allowsUpToTheLimitThenDenies() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(Clock.systemUTC());
        try {
            assertThat(limiter.tryAcquire("k", THREE_PER_MINUTE).allowed()).isTrue();
            assertThat(limiter.tryAcquire("k", THREE_PER_MINUTE).allowed()).isTrue();
            assertThat(limiter.tryAcquire("k", THREE_PER_MINUTE).allowed()).isTrue();

            RateLimitResult denied = limiter.tryAcquire("k", THREE_PER_MINUTE);
            assertThat(denied.allowed()).isFalse();
            assertThat(denied.retryAfterSeconds()).isPositive();
        } finally {
            limiter.shutdown();
        }
    }

    @Test
    void startsAFreshWindowAfterTheCurrentOneElapses() {
        MutableClock clock = new MutableClock(Instant.parse("2026-11-22T00:00:00Z"));
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(clock);
        try {
            for (int i = 0; i < 3; i++) {
                assertThat(limiter.tryAcquire("k", THREE_PER_MINUTE).allowed()).isTrue();
            }
            assertThat(limiter.tryAcquire("k", THREE_PER_MINUTE).allowed()).isFalse();

            clock.advance(Duration.ofSeconds(61));

            assertThat(limiter.tryAcquire("k", THREE_PER_MINUTE).allowed()).isTrue();
        } finally {
            limiter.shutdown();
        }
    }

    @Test
    void keepsSeparateCountersPerKey() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(Clock.systemUTC());
        try {
            for (int i = 0; i < 3; i++) {
                limiter.tryAcquire("a", THREE_PER_MINUTE);
            }
            assertThat(limiter.tryAcquire("a", THREE_PER_MINUTE).allowed()).isFalse();
            assertThat(limiter.tryAcquire("b", THREE_PER_MINUTE).allowed()).isTrue();
        } finally {
            limiter.shutdown();
        }
    }

    @Test
    void admitsExactlyTheLimitUnderConcurrency() throws Exception {
        int limit = 50;
        int threads = 500;
        RateLimitPolicy policy = new RateLimitPolicy(limit, Duration.ofMinutes(1));
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(Clock.systemUTC());
        ExecutorService pool = Executors.newFixedThreadPool(32);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger allowed = new AtomicInteger();

        try {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    try {
                        start.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    if (limiter.tryAcquire("same-key", policy).allowed()) {
                        allowed.incrementAndGet();
                    }
                });
            }
            start.countDown();
            pool.shutdown();
            assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

            // Atomic counting means the limit is exact, never over-admitted.
            assertThat(allowed.get()).isEqualTo(limit);
        } finally {
            limiter.shutdown();
        }
    }

    /** A clock whose instant can be moved forward, for window-expiry tests. */
    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            this.instant = this.instant.plus(duration);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
