package com.reverie.account;

/**
 * The inputs the account module needs to register a user. The web layer maps its
 * validated request DTO into this command, so the account module does not depend
 * on web types. Values are already syntactically valid; the service normalizes
 * the email and enforces uniqueness.
 */
public record RegistrationCommand(String email, String password, String displayName, String username) {
}
