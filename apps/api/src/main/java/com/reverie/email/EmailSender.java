package com.reverie.email;

/**
 * Port for sending transactional email (ADR-005).
 *
 * <p>Milestone 1 ships one SMTP-backed implementation that targets Mailpit
 * locally. A production provider implements the same port later without changing
 * callers. Keeping this interface narrow (a single {@code send}) means content
 * decisions live with the caller, not the transport.
 */
public interface EmailSender {

    void send(EmailMessage message);
}
