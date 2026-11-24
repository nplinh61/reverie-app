package com.reverie.web.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for resending a verification email. It takes only the email so
 * the endpoint works before login. The response is neutral regardless of whether
 * an account exists for the address (ADR-005).
 */
public record ResendVerificationRequest(
        @NotBlank @Email @Size(max = 255) String email) {
}
