package com.reverie.ratelimit;

/**
 * Outcome of a rate-limit check. When {@code allowed} is false,
 * {@code retryAfterSeconds} is a positive hint the caller can place in a
 * {@code Retry-After} header.
 */
public record RateLimitResult(boolean allowed, long retryAfterSeconds) {

    public static RateLimitResult allow() {
        return new RateLimitResult(true, 0);
    }

    public static RateLimitResult deny(long retryAfterSeconds) {
        return new RateLimitResult(false, retryAfterSeconds);
    }
}
