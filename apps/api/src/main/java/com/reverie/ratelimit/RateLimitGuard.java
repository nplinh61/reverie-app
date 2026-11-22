package com.reverie.ratelimit;

import org.springframework.stereotype.Component;

/**
 * Applies the registration and resend rate limits, throwing
 * {@link RateLimitExceededException} when a limit is exceeded. Centralizing the
 * key format here keeps it consistent and keeps the services free of
 * limiter wiring.
 *
 * <p>Checks run before any account lookup and the keys do not depend on whether
 * an account exists, so the limiter never leaks account existence: the response
 * is a uniform 429 either way.
 */
@Component
public class RateLimitGuard {

    private final RateLimiter rateLimiter;
    private final RateLimitProperties properties;

    public RateLimitGuard(RateLimiter rateLimiter, RateLimitProperties properties) {
        this.rateLimiter = rateLimiter;
        this.properties = properties;
    }

    /** Limits registration attempts per client IP. */
    public void checkRegistration(String clientIp) {
        enforce("register:ip:" + clientIp, properties.getRegisterPerIp().toPolicy());
    }

    /** Limits resend attempts per client IP and per submitted email. */
    public void checkResend(String clientIp, String normalizedEmail) {
        enforce("resend:ip:" + clientIp, properties.getResendPerIp().toPolicy());
        enforce("resend:email:" + normalizedEmail, properties.getResendPerEmail().toPolicy());
    }

    private void enforce(String key, RateLimitPolicy policy) {
        RateLimitResult result = rateLimiter.tryAcquire(key, policy);
        if (!result.allowed()) {
            throw new RateLimitExceededException(result.retryAfterSeconds());
        }
    }
}
