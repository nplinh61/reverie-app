package com.reverie.web.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body carrying the raw verification token from the emailed link. The
 * token is validated for presence only; whether it is genuine, unexpired, and
 * unconsumed is decided by the atomic consume in the service.
 */
public record VerifyEmailRequest(@NotBlank String token) {
}
