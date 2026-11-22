package com.reverie.account;

/**
 * Internal signal that the email is already registered. It is caught inside the
 * registration flow to drive the neutral duplicate-email path (no second
 * account, an "account already exists" notice email). It is never shown to the
 * client, because doing so would disclose that the email exists (ADR-005,
 * SECURITY.md section 2).
 */
public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException() {
        super("Email already registered");
    }
}
