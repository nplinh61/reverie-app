package com.reverie.account;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Email verification settings bound from {@code reverie.verification} (ADR-005).
 *
 * <p>{@code baseUrl} is the public origin used to build the verification link in
 * the email. {@code tokenValidity} is how long a token can be used.
 * {@code resendCooldown} is the smallest gap allowed between two verification
 * emails to the same account; it is the persistent half of the abuse protection
 * and holds even across a restart, unlike the in-memory rate limiter.
 */
@ConfigurationProperties(prefix = "reverie.verification")
public class VerificationProperties {

    private String baseUrl = "http://localhost:4200";
    private Duration tokenValidity = Duration.ofHours(24);
    private Duration resendCooldown = Duration.ofSeconds(60);

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Duration getTokenValidity() {
        return tokenValidity;
    }

    public void setTokenValidity(Duration tokenValidity) {
        this.tokenValidity = tokenValidity;
    }

    public Duration getResendCooldown() {
        return resendCooldown;
    }

    public void setResendCooldown(Duration resendCooldown) {
        this.resendCooldown = resendCooldown;
    }
}
