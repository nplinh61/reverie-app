package com.reverie.account;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Persistence for {@link User} aggregates. Spring Data JDBC implements the
 * {@link CrudRepository} methods; the explicit queries below use the normalized
 * email and, for resend, a row lock to serialize concurrent requests.
 */
public interface UserRepository extends CrudRepository<User, UUID> {

    /** Looks up a user by their already-normalized email. */
    @Query("SELECT * FROM users WHERE email = :email")
    Optional<User> findByEmail(@Param("email") String email);

    /**
     * Looks up a user by email and takes a row lock (FOR UPDATE) for the duration
     * of the surrounding transaction. Used by resend so the cooldown check and
     * token issue cannot interleave for the same account (ADR-005).
     */
    @Query("SELECT * FROM users WHERE email = :email FOR UPDATE")
    Optional<User> findByEmailForUpdate(@Param("email") String email);
}
