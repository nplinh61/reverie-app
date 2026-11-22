package com.reverie.web.auth;

import com.reverie.web.auth.validation.Password;
import com.reverie.web.auth.validation.Username;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Registration request body. Bean Validation runs at the controller boundary;
 * a failure here becomes a 400 {@code VALIDATION_FAILED} with field errors and
 * never reaches the service. The service then normalizes the email and performs
 * the uniqueness checks the database ultimately enforces.
 */
public record RegisterRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Password String password,
        @NotBlank @Size(max = 100) String displayName,
        @NotBlank @Username String username) {
}
