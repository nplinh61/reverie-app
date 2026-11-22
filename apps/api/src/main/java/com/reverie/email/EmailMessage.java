package com.reverie.email;

/**
 * A plain-text email to send. The transport adds the sender identity from
 * configuration, so callers provide only the recipient, subject, and body.
 */
public record EmailMessage(String to, String subject, String body) {
}
