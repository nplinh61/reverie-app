package com.reverie.account;

import com.reverie.persistence.AbstractAggregateRoot;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A single-use email verification token (DATABASE_ERD section 3.1.1, ADR-005).
 *
 * <p>Only the SHA-256 hash of the token is stored. The raw token exists only in
 * the emailed link and is never persisted, returned, or logged. Consumption is
 * performed by an atomic conditional update (see
 * {@link EmailVerificationTokenQueries}), so there is no mutating {@code consume}
 * method on the aggregate itself.
 */
@Table("email_verification_tokens")
public class EmailVerificationToken extends AbstractAggregateRoot {

    @Column("user_id")
    private final UUID userId;

    @Column("token_hash")
    private final String tokenHash;

    @Column("expires_at")
    private final OffsetDateTime expiresAt;

    @Column("consumed_at")
    private final OffsetDateTime consumedAt;

    @Column("created_at")
    private final OffsetDateTime createdAt;

    @PersistenceCreator
    EmailVerificationToken(UUID id, UUID userId, String tokenHash, OffsetDateTime expiresAt,
            OffsetDateTime consumedAt, OffsetDateTime createdAt) {
        super(id);
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.consumedAt = consumedAt;
        this.createdAt = createdAt;
    }

    /** Issues a fresh, unconsumed token that expires after the given validity window. */
    public static EmailVerificationToken issue(UUID userId, String tokenHash,
            OffsetDateTime now, Duration validity) {
        return new EmailVerificationToken(UUID.randomUUID(), userId, tokenHash,
                now.plus(validity), null, now);
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getConsumedAt() {
        return consumedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
