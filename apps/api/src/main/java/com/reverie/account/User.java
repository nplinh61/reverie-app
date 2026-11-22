package com.reverie.account;

import com.reverie.persistence.AbstractAggregateRoot;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A user account: authentication identity and account state (DATABASE_ERD
 * section 3.1). The user-facing handle and display name live on the separate
 * {@link Profile} aggregate.
 *
 * <p>The aggregate is immutable. {@code emailVerifiedAt} is set by an atomic
 * database update during verification rather than by mutating a loaded instance,
 * so there is no setter here. {@code email} is already normalized (trimmed and
 * lowercased) by the service before construction.
 */
@Table("users")
public class User extends AbstractAggregateRoot {

    private final String email;

    @Column("password_hash")
    private final String passwordHash;

    @Column("email_verified_at")
    private final OffsetDateTime emailVerifiedAt;

    private final boolean enabled;

    @Column("created_at")
    private final OffsetDateTime createdAt;

    @Column("updated_at")
    private final OffsetDateTime updatedAt;

    @PersistenceCreator
    User(UUID id, String email, String passwordHash, OffsetDateTime emailVerifiedAt,
            boolean enabled, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        super(id);
        this.email = email;
        this.passwordHash = passwordHash;
        this.emailVerifiedAt = emailVerifiedAt;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Creates a brand-new, enabled but unverified account. The account can
     * authenticate before verification; {@code emailVerifiedAt} stays null until
     * the user verifies their email (ADR-003, ADR-005).
     */
    public static User register(String normalizedEmail, String passwordHash, OffsetDateTime now) {
        return new User(UUID.randomUUID(), normalizedEmail, passwordHash, null, true, now, now);
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public OffsetDateTime getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
