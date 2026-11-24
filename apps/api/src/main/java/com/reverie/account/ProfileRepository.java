package com.reverie.account;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Persistence for {@link Profile} aggregates.
 *
 * <p>The username availability pre-check is case-insensitive, matching the
 * unique index on {@code lower(username)}. The pre-check only avoids the common
 * case; the database index remains the authoritative guard under concurrent
 * registration (API_DESIGN.md section 2).
 */
public interface ProfileRepository extends CrudRepository<Profile, UUID> {

    @Query("SELECT count(*) FROM profiles WHERE lower(username) = lower(:username)")
    long countByUsernameIgnoreCase(@Param("username") String username);

    @Query("SELECT * FROM profiles WHERE user_id = :userId")
    Optional<Profile> findByUserId(@Param("userId") UUID userId);

    default boolean existsByUsernameIgnoreCase(String username) {
        return countByUsernameIgnoreCase(username) > 0;
    }
}
