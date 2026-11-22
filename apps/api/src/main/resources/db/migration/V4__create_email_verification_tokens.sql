-- V4 email_verification_tokens: single-use email verification tokens
-- (DATABASE_ERD section 3.1.1, ADR-005).
--
-- The raw token is delivered only in the verification email. Only its SHA-256
-- hash is stored here, so a database leak never reveals a usable token. A fast
-- cryptographic hash is appropriate because the token is high entropy, unlike a
-- password. token_hash is UNIQUE so a hash maps to at most one row, which lets
-- verification consume it with a single conditional UPDATE.
--
-- Tokens are single-use and expire after 24 hours. consumed_at is set exactly
-- once, by the atomic UPDATE that also marks the user verified. A resend
-- invalidates prior unconsumed tokens for the same user.
CREATE TABLE email_verification_tokens (
    id          UUID         NOT NULL,
    user_id     UUID         NOT NULL,
    token_hash  VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    consumed_at TIMESTAMPTZ  NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_email_verification_tokens PRIMARY KEY (id),
    CONSTRAINT uq_email_verification_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_email_verification_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- Supports resend invalidation and cooldown lookups, which filter by user.
CREATE INDEX idx_email_verification_tokens_user ON email_verification_tokens (user_id);
