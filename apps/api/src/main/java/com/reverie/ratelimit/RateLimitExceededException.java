package com.reverie.ratelimit;

/**
 * Thrown when a request exceeds a rate limit. The web layer maps this to a 429
 * response with a generic body and a {@code Retry-After} header built from
 * {@link #getRetryAfterSeconds()}. It never discloses which bucket was hit.
 */
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super("Rate limit exceeded");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
