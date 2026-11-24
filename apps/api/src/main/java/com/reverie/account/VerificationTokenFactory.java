package com.reverie.account;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/**
 * Creates verification tokens and hashes them for storage (ADR-005).
 *
 * <p>The raw token is 32 cryptographically random bytes, URL-safe Base64 encoded
 * so it travels cleanly in a link. Only its SHA-256 hash is stored. A fast
 * cryptographic hash is correct here because the token is high entropy, unlike a
 * password, so no salt or slow hash is needed. The raw token is returned to the
 * caller only to be emailed and is never persisted or logged.
 */
@Component
public class VerificationTokenFactory {

    private static final int TOKEN_BYTES = 32;
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final SecureRandom secureRandom = new SecureRandom();

    /** A fresh, unguessable raw token. Give this to the user, never to the database. */
    public String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return URL_ENCODER.encodeToString(bytes);
    }

    /** The SHA-256 hash (lowercase hex) of a raw token. This is what the database stores. */
    public String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required but not available", e);
        }
    }
}
