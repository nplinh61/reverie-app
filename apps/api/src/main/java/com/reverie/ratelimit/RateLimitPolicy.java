package com.reverie.ratelimit;

import java.time.Duration;

/**
 * A rate-limit rule: at most {@code max} requests per {@code window}, measured
 * as a fixed window. This is the value a caller hands to the {@link RateLimiter};
 * configuration is bound separately in {@link RateLimitProperties}.
 */
public record RateLimitPolicy(int max, Duration window) {
}
