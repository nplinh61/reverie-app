package com.reverie.account;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Unit test for {@link VerificationTokenFactory}. */
class VerificationTokenFactoryTest {

    private final VerificationTokenFactory factory = new VerificationTokenFactory();

    @Test
    void generatesDifferentTokensEachTime() {
        assertThat(factory.generateRawToken()).isNotEqualTo(factory.generateRawToken());
    }

    @Test
    void hashingIsDeterministic() {
        String raw = factory.generateRawToken();
        assertThat(factory.hash(raw)).isEqualTo(factory.hash(raw));
    }

    @Test
    void hashIsNotTheRawToken() {
        String raw = factory.generateRawToken();
        assertThat(factory.hash(raw)).isNotEqualTo(raw);
    }

    @Test
    void hashIsSha256Hex() {
        // SHA-256 is 32 bytes, which is 64 lowercase hex characters.
        assertThat(factory.hash("anything")).hasSize(64).matches("[0-9a-f]{64}");
    }
}
