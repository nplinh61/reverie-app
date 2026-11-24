package com.reverie.account;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Explicit SQL for the verification-token operations that must be atomic or that
 * need a condition the aggregate cannot express (ADR-002, ADR-005). Kept
 * separate from {@link EmailVerificationTokenRepository} so the simple insert
 * stays with the standard repository and the careful operations are obvious.
 */
@Component
public class EmailVerificationTokenQueries {

    private final JdbcClient jdbcClient;

    public EmailVerificationTokenQueries(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * Consumes a token atomically and returns the owning user id.
     *
     * <p>The single conditional UPDATE sets {@code consumed_at} only when the
     * token is still unconsumed and unexpired, and {@code RETURNING user_id}
     * reports whether this exact statement did the work. Row locking makes
     * concurrent verification of the same token succeed exactly once: the first
     * request sets {@code consumed_at}, and the second then matches zero rows.
     * An empty result means the token was unknown, expired, or already consumed.
     */
    public Optional<UUID> consumeAndReturnUserId(String tokenHash, OffsetDateTime now) {
        return jdbcClient.sql("""
                        UPDATE email_verification_tokens
                        SET consumed_at = :now
                        WHERE token_hash = :tokenHash
                          AND consumed_at IS NULL
                          AND expires_at > :now
                        RETURNING user_id
                        """)
                .param("tokenHash", tokenHash)
                .param("now", now)
                .query(UUID.class)
                .optional();
    }

    /**
     * Invalidates every still-unconsumed token for a user, so a resend leaves
     * exactly one usable token. The caller runs this inside the same transaction
     * that holds the user row lock.
     */
    public int invalidateUnconsumedForUser(UUID userId, OffsetDateTime now) {
        return jdbcClient.sql("""
                        UPDATE email_verification_tokens
                        SET consumed_at = :now
                        WHERE user_id = :userId
                          AND consumed_at IS NULL
                        """)
                .param("userId", userId)
                .param("now", now)
                .update();
    }

    /** The most recent token creation time for a user, used for the resend cooldown. */
    public Optional<OffsetDateTime> findLatestCreatedAt(UUID userId) {
        return jdbcClient.sql("""
                        SELECT created_at
                        FROM email_verification_tokens
                        WHERE user_id = :userId
                        ORDER BY created_at DESC
                        LIMIT 1
                        """)
                .param("userId", userId)
                .query(OffsetDateTime.class)
                .optional();
    }
}
