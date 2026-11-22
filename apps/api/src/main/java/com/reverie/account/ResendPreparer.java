package com.reverie.account;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The transactional part of resend (ADR-005). It runs in its own bean so the
 * transaction and its row lock are a real proxy call, and so the email is sent by
 * the caller only after the transaction commits, never while the lock is held.
 *
 * <p>The user row is locked with FOR UPDATE for the duration of the transaction.
 * That makes the cooldown check and the token issue a single serialized unit:
 * two concurrent resends for the same account cannot both pass the cooldown and
 * both send, which a plain check-then-insert would allow. Every "do nothing"
 * branch returns empty so the caller's response stays neutral.
 */
@Component
public class ResendPreparer {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailVerificationTokenQueries tokenQueries;
    private final VerificationTokenFactory tokenFactory;
    private final VerificationProperties verificationProperties;
    private final Clock clock;

    public ResendPreparer(UserRepository userRepository, ProfileRepository profileRepository,
            EmailVerificationTokenRepository tokenRepository, EmailVerificationTokenQueries tokenQueries,
            VerificationTokenFactory tokenFactory, VerificationProperties verificationProperties,
            Clock clock) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.tokenRepository = tokenRepository;
        this.tokenQueries = tokenQueries;
        this.tokenFactory = tokenFactory;
        this.verificationProperties = verificationProperties;
        this.clock = clock;
    }

    @Transactional
    public Optional<IssuedVerification> prepareResend(String normalizedEmail) {
        Optional<User> maybeUser = userRepository.findByEmailForUpdate(normalizedEmail);
        if (maybeUser.isEmpty()) {
            return Optional.empty();
        }
        User user = maybeUser.get();
        if (user.isEmailVerified()) {
            // Nothing to resend; stay neutral rather than revealing the account is verified.
            return Optional.empty();
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        Duration cooldown = verificationProperties.getResendCooldown();
        Optional<OffsetDateTime> lastIssued = tokenQueries.findLatestCreatedAt(user.getId());
        if (lastIssued.isPresent() && Duration.between(lastIssued.get(), now).compareTo(cooldown) < 0) {
            return Optional.empty();
        }

        tokenQueries.invalidateUnconsumedForUser(user.getId(), now);
        String rawToken = tokenFactory.generateRawToken();
        tokenRepository.save(EmailVerificationToken.issue(
                user.getId(), tokenFactory.hash(rawToken), now, verificationProperties.getTokenValidity()));

        String displayName = profileRepository.findByUserId(user.getId())
                .map(Profile::getDisplayName)
                .orElse("there");
        return Optional.of(new IssuedVerification(normalizedEmail, displayName, rawToken));
    }
}
