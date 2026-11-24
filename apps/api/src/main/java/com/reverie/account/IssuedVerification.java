package com.reverie.account;

/**
 * A verification email waiting to be sent: the recipient, their display name for
 * the greeting, and the raw token for the link. Produced inside a transaction
 * and handed to the mail service only after the transaction commits, so the raw
 * token never lives longer than it must and is never persisted.
 */
public record IssuedVerification(String email, String displayName, String rawToken) {
}
