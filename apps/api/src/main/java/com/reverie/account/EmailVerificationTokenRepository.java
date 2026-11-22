package com.reverie.account;

import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

/**
 * Persistence for {@link EmailVerificationToken} aggregates. Only the simple
 * insert is needed here; the atomic consume, the resend invalidation, and the
 * cooldown lookup are explicit SQL in {@link EmailVerificationTokenQueries}
 * (ADR-002, ADR-005).
 */
public interface EmailVerificationTokenRepository extends CrudRepository<EmailVerificationToken, UUID> {
}
