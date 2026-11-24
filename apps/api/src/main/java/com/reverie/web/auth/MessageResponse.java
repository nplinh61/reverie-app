package com.reverie.web.auth;

/**
 * A small response body carrying only a human-readable message. Used by the
 * registration, resend, and verification endpoints, whose responses are
 * intentionally neutral and reveal nothing about account existence.
 */
public record MessageResponse(String message) {
}
