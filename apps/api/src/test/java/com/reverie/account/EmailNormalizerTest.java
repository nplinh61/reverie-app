package com.reverie.account;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Unit test for {@link EmailNormalizer}. */
class EmailNormalizerTest {

    @Test
    void trimsAndLowercases() {
        assertThat(EmailNormalizer.normalize("  Alice@Example.COM  ")).isEqualTo("alice@example.com");
    }

    @Test
    void leavesAnAlreadyNormalizedAddressUnchanged() {
        assertThat(EmailNormalizer.normalize("alice@example.com")).isEqualTo("alice@example.com");
    }

    @Test
    void returnsNullForNull() {
        assertThat(EmailNormalizer.normalize(null)).isNull();
    }
}
