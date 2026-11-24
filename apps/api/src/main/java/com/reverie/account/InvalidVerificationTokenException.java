package com.reverie.account;

/**
 * Thrown when a verification token is unknown, expired, or already consumed. The
 * web layer maps all three to one uniform response so it never discloses which
 * case applied (ADR-005).
 */
public class InvalidVerificationTokenException extends RuntimeException {

    public InvalidVerificationTokenException() {
        super("Invalid or expired verification token");
    }
}
