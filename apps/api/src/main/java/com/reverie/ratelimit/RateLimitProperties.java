package com.reverie.ratelimit;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Rate-limit settings bound from {@code reverie.rate-limit}. Each entry is a
 * max count and a window; {@link Policy#toPolicy()} converts it into the
 * immutable {@link RateLimitPolicy} that the limiter consumes.
 *
 * <p>Numbers are deliberately conservative starting points and are expected to
 * be tuned (M1 spec section 13 treats concrete limits as non-blocking).
 */
@ConfigurationProperties(prefix = "reverie.rate-limit")
public class RateLimitProperties {

    private Policy registerPerIp = new Policy(5, Duration.ofMinutes(10));
    private Policy resendPerIp = new Policy(10, Duration.ofHours(1));
    private Policy resendPerEmail = new Policy(3, Duration.ofHours(1));

    public Policy getRegisterPerIp() {
        return registerPerIp;
    }

    public void setRegisterPerIp(Policy registerPerIp) {
        this.registerPerIp = registerPerIp;
    }

    public Policy getResendPerIp() {
        return resendPerIp;
    }

    public void setResendPerIp(Policy resendPerIp) {
        this.resendPerIp = resendPerIp;
    }

    public Policy getResendPerEmail() {
        return resendPerEmail;
    }

    public void setResendPerEmail(Policy resendPerEmail) {
        this.resendPerEmail = resendPerEmail;
    }

    /** A bound max/window pair. Mutable so Spring Boot can bind it from YAML. */
    public static class Policy {

        private int max;
        private Duration window;

        public Policy() {
        }

        public Policy(int max, Duration window) {
            this.max = max;
            this.window = window;
        }

        public int getMax() {
            return max;
        }

        public void setMax(int max) {
            this.max = max;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }

        public RateLimitPolicy toPolicy() {
            return new RateLimitPolicy(max, window);
        }
    }
}
