package com.reverie.account;

/**
 * Thrown when a requested username is already in use. A username is a public
 * handle, not a secret, so the web layer reports this as a 400
 * {@code VALIDATION_FAILED} with a field error on {@code username}, rather than
 * a neutral response (API_DESIGN.md section 2, SECURITY.md section 2).
 */
public class UsernameAlreadyTakenException extends RuntimeException {

    public UsernameAlreadyTakenException() {
        super("Username already taken");
    }
}
