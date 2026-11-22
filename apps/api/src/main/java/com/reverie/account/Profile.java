package com.reverie.account;

import com.reverie.persistence.AbstractAggregateRoot;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A user's public-facing profile (DATABASE_ERD section 3.2). It holds the unique
 * username (a public handle), the display name, and an optional bio. One profile
 * belongs to exactly one {@link User} through {@code userId}.
 *
 * <p>The username is stored already lowercased (username policy, M1 spec section
 * 3) and its case-insensitive uniqueness is guarded by a unique index on
 * {@code lower(username)}. {@code avatarMediaId} stays null until Milestone 3
 * introduces media.
 */
@Table("profiles")
public class Profile extends AbstractAggregateRoot {

    @Column("user_id")
    private final UUID userId;

    private final String username;

    @Column("display_name")
    private final String displayName;

    private final String bio;

    @Column("avatar_media_id")
    private final UUID avatarMediaId;

    @Column("created_at")
    private final OffsetDateTime createdAt;

    @Column("updated_at")
    private final OffsetDateTime updatedAt;

    @PersistenceCreator
    Profile(UUID id, UUID userId, String username, String displayName, String bio,
            UUID avatarMediaId, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        super(id);
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.bio = bio;
        this.avatarMediaId = avatarMediaId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Creates a new profile for a just-registered user. Avatar is set later in M3. */
    public static Profile create(UUID userId, String username, String displayName,
            String bio, OffsetDateTime now) {
        return new Profile(UUID.randomUUID(), userId, username, displayName, bio, null, now, now);
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBio() {
        return bio;
    }

    public UUID getAvatarMediaId() {
        return avatarMediaId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
