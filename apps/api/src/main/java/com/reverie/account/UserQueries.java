package com.reverie.account;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Explicit SQL for user updates that are not a whole-aggregate save. Verification
 * marks the user verified with a conditional update that only sets the timestamp
 * once, so repeated verification is harmless.
 */
@Component
public class UserQueries {

    private final JdbcClient jdbcClient;

    public UserQueries(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * Marks a user's email verified. The {@code email_verified_at IS NULL} guard
     * makes it idempotent: a second call changes nothing and returns zero. Runs in
     * the same transaction as the token consume so the two succeed together.
     */
    public int markVerified(UUID userId, OffsetDateTime now) {
        return jdbcClient.sql("""
                        UPDATE users
                        SET email_verified_at = :now, updated_at = :now
                        WHERE id = :id AND email_verified_at IS NULL
                        """)
                .param("now", now)
                .param("id", userId)
                .update();
    }
}
