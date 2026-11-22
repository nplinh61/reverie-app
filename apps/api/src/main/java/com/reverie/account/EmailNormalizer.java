package com.reverie.account;

import java.util.Locale;

/**
 * Normalizes email addresses by trimming surrounding whitespace and lowercasing.
 *
 * <p>The same normalization is applied for registration, login, and
 * email-specific invitation matching (ADR-003, SECURITY.md section 2), so stored
 * emails are already normalized and the database UNIQUE constraint enforces
 * case-insensitive uniqueness. Provider-specific normalization (for example
 * dropping dots in Gmail addresses) is intentionally not implemented.
 */
public final class EmailNormalizer {

    private EmailNormalizer() {
    }

    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
