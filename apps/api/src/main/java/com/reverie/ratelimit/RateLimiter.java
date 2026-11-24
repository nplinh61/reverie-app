package com.reverie.ratelimit;

/**
 * A minimal rate limiter port.
 *
 * <p>Milestone 1 ships a single in-memory implementation
 * ({@link InMemoryRateLimiter}). This interface is the seam that lets a
 * distributed implementation (for example Bucket4j) replace it in a later phase
 * without changing callers, mirroring the {@code EmailSender} port pattern.
 */
public interface RateLimiter {

    /**
     * Counts one request against {@code key} and reports whether it is allowed
     * under {@code policy}. The key encodes both the action and the dimension,
     * for example {@code "register:ip:203.0.113.5"}, so unrelated limits never
     * share a counter.
     */
    RateLimitResult tryAcquire(String key, RateLimitPolicy policy);
}
