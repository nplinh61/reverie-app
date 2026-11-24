package com.reverie.web.auth.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link PasswordValidator}. It validates a tiny record carrying
 * one {@code @Password} field, so it exercises the real constraint wiring rather
 * than calling the validator directly. The byte limit is checked with a
 * multi-byte character, which is where a character count and a byte count
 * disagree.
 */
class PasswordValidatorTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private record Holder(@Password String password) {
    }

    private boolean valid(String password) {
        return validator.validate(new Holder(password)).isEmpty();
    }

    @Test
    void acceptsAtLeastTenCharacters() {
        assertThat(valid("1234567890")).isTrue();
    }

    @Test
    void rejectsShorterThanTenCharacters() {
        assertThat(valid("123456789")).isFalse();
    }

    @Test
    void acceptsExactlySeventyTwoBytes() {
        assertThat(valid("a".repeat(72))).isTrue();
    }

    @Test
    void rejectsMoreThanSeventyTwoBytes() {
        assertThat(valid("a".repeat(73))).isFalse();
    }

    @Test
    void measuresTheLimitInBytesNotCharacters() {
        // Each "é" is two UTF-8 bytes. 36 of them are 36 characters but 72 bytes
        // (allowed); 37 are 37 characters but 74 bytes (rejected).
        String thirtySix = "é".repeat(36);
        String thirtySeven = "é".repeat(37);

        assertThat(thirtySix.getBytes(StandardCharsets.UTF_8).length).isEqualTo(72);
        assertThat(thirtySeven.getBytes(StandardCharsets.UTF_8).length).isEqualTo(74);
        assertThat(valid(thirtySix)).isTrue();
        assertThat(valid(thirtySeven)).isFalse();
    }

    @Test
    void ignoresNullSoNotBlankCanOwnIt() {
        // The constraint itself passes null through; @NotBlank is responsible for it.
        assertThat(valid(null)).isTrue();
    }
}
