package com.reverie.ratelimit;

import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * A fixed-window in-memory rate limiter.
 *
 * <p>IMPORTANT: this is single-instance only. Counters live in the memory of one
 * process and reset on restart, so it is a minimal abuse safeguard rather than a
 * strong distributed control. It must be revisited before any multi-instance or
 * public deployment, where it is intended to be replaced by a shared store (for
 * example Bucket4j) behind the {@link RateLimiter} port.
 *
 * <p>Concurrency: each key's counter is updated inside
 * {@link ConcurrentHashMap#compute}, whose remapping function runs atomically
 * for that key. Two simultaneous requests for the same key cannot both read a
 * stale count, so the limit holds under concurrency.
 *
 * <p>Memory: a key lives only for the length of its window. A background sweep
 * removes expired windows, and a hard {@code MAX_ENTRIES} cap bounds the map even
 * under a flood of distinct keys. If the cap is reached the limiter fails open
 * (allows the request without tracking it) so limiter memory pressure can never
 * lock out legitimate users.
 */
@Component
public class InMemoryRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(InMemoryRateLimiter.class);

    /** Upper bound on tracked keys, so the map cannot grow without limit. */
    private static final int MAX_ENTRIES = 100_000;

    /** How often expired windows are swept out of the map. */
    private static final long SWEEP_PERIOD_SECONDS = 60;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;
    private final ScheduledExecutorService sweeper;

    public InMemoryRateLimiter(Clock clock) {
        this.clock = clock;
        this.sweeper = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "rate-limit-sweeper");
            thread.setDaemon(true);
            return thread;
        });
        this.sweeper.scheduleAtFixedRate(
                () -> sweepExpired(clock.millis()),
                SWEEP_PERIOD_SECONDS, SWEEP_PERIOD_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public RateLimitResult tryAcquire(String key, RateLimitPolicy policy) {
        long nowMillis = clock.millis();
        long windowMillis = policy.window().toMillis();
        int max = policy.max();

        if (windows.size() >= MAX_ENTRIES && !windows.containsKey(key)) {
            sweepExpired(nowMillis);
            if (windows.size() >= MAX_ENTRIES && !windows.containsKey(key)) {
                log.warn("Rate limiter at capacity ({} keys); allowing request without tracking", MAX_ENTRIES);
                return RateLimitResult.allow();
            }
        }

        // One-element holders let the atomic remapping function report its decision.
        boolean[] allowed = {false};
        long[] retryAfterSeconds = {0};

        windows.compute(key, (k, existing) -> {
            if (existing == null || nowMillis >= existing.expiresAtMillis()) {
                // No window, or the previous one elapsed: start a fresh window.
                allowed[0] = true;
                return new Window(nowMillis + windowMillis, 1);
            }
            if (existing.count() < max) {
                allowed[0] = true;
                return new Window(existing.expiresAtMillis(), existing.count() + 1);
            }
            // Limit reached: keep the window unchanged and deny.
            allowed[0] = false;
            long remainingMillis = existing.expiresAtMillis() - nowMillis;
            retryAfterSeconds[0] = Math.max(1, (remainingMillis + 999) / 1000);
            return existing;
        });

        return allowed[0] ? RateLimitResult.allow() : RateLimitResult.deny(retryAfterSeconds[0]);
    }

    private void sweepExpired(long nowMillis) {
        windows.values().removeIf(window -> nowMillis >= window.expiresAtMillis());
    }

    @PreDestroy
    void shutdown() {
        sweeper.shutdownNow();
    }

    /** An immutable fixed window: when it ends, and how many requests it has counted. */
    private record Window(long expiresAtMillis, int count) {
    }
}
