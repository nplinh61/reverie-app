package com.reverie.account;

import com.reverie.ratelimit.RateLimitGuard;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles email verification and resend (ADR-005).
 *
 * <p>{@link #verify} is transactional: it consumes the token and marks the user
 * verified in one unit, so concurrent attempts with the same token succeed
 * exactly once. {@link #resend} is not transactional itself; it rate-limits,
 * delegates the locked database work to {@link ResendPreparer}, and sends the
 * email only after that transaction commits.
 */
@Service
public class EmailVerificationService {

    private final EmailVerificationTokenQueries tokenQueries;
    private final UserQueries userQueries;
    private final VerificationTokenFactory tokenFactory;
    private final ResendPreparer resendPreparer;
    private final AccountMailService mailService;
    private final RateLimitGuard rateLimitGuard;
    private final Clock clock;

    public EmailVerificationService(EmailVerificationTokenQueries tokenQueries, UserQueries userQueries,
            VerificationTokenFactory tokenFactory, ResendPreparer resendPreparer,
            AccountMailService mailService, RateLimitGuard rateLimitGuard, Clock clock) {
        this.tokenQueries = tokenQueries;
        this.userQueries = userQueries;
        this.tokenFactory = tokenFactory;
        this.resendPreparer = resendPreparer;
        this.mailService = mailService;
        this.rateLimitGuard = rateLimitGuard;
        this.clock = clock;
    }

    /**
     * Verifies an email from its raw token. Consuming the token and marking the
     * user verified happen in the same transaction. An unknown, expired, or
     * already-consumed token yields {@link InvalidVerificationTokenException}.
     */
    @Transactional
    public void verify(String rawToken) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        String tokenHash = tokenFactory.hash(rawToken);
        UUID userId = tokenQueries.consumeAndReturnUserId(tokenHash, now)
                .orElseThrow(InvalidVerificationTokenException::new);
        userQueries.markVerified(userId, now);
    }

    /** Resends a verification email if appropriate. Always neutral to the caller. */
    public void resend(String rawEmail, String clientIp) {
        String email = EmailNormalizer.normalize(rawEmail);
        rateLimitGuard.checkResend(clientIp, email);
        resendPreparer.prepareResend(email).ifPresent(issued ->
                mailService.sendVerificationEmail(issued.email(), issued.displayName(), issued.rawToken()));
    }
}
