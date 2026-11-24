-- V3 profiles: user-facing profile information (DATABASE_ERD section 3.2).
--
-- Each user has exactly one profile, enforced by the UNIQUE(user_id). The
-- username is a public handle. Its uniqueness is case-insensitive, enforced by a
-- unique index on lower(username) rather than on the raw column, so "Alice" and
-- "alice" cannot both exist. The application stores usernames already lowercased
-- (username policy, M1 spec section 3), and this index is the authoritative guard
-- under concurrent registration.
--
-- avatar_media_id is created now as a nullable column with no foreign key. The
-- media table does not exist until Milestone 3, so its foreign-key constraint is
-- added later by ALTER TABLE (DATABASE_ERD section 8).
CREATE TABLE profiles (
    id              UUID         NOT NULL,
    user_id         UUID         NOT NULL,
    username        VARCHAR(30)  NOT NULL,
    display_name    VARCHAR(100) NOT NULL,
    bio             VARCHAR(500) NULL,
    avatar_media_id UUID         NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_profiles PRIMARY KEY (id),
    CONSTRAINT uq_profiles_user_id UNIQUE (user_id),
    CONSTRAINT fk_profiles_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- Case-insensitive uniqueness of the username.
CREATE UNIQUE INDEX uq_profiles_username_lower ON profiles (lower(username));
