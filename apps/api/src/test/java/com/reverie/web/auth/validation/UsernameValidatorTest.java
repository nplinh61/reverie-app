package com.reverie.web.auth.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Unit test for {@link UsernameValidator} against the Milestone 1 username policy. */
class UsernameValidatorTest {

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

    private record Holder(@Username String username) {
    }

    private boolean valid(String username) {
        return validator.validate(new Holder(username)).isEmpty();
    }

    @Test
    void acceptsLowercaseLettersDigitsAndUnderscores() {
        assertThat(valid("alice_01")).isTrue();
    }

    @Test
    void acceptsLeadingAndTrailingUnderscores() {
        assertThat(valid("_alice_")).isTrue();
    }

    @Test
    void rejectsTooShort() {
        assertThat(valid("ab")).isFalse();
    }

    @Test
    void rejectsTooLong() {
        assertThat(valid("a".repeat(31))).isFalse();
    }

    @Test
    void rejectsUppercase() {
        assertThat(valid("Alice")).isFalse();
    }

    @Test
    void rejectsOtherPunctuation() {
        assertThat(valid("al.ice")).isFalse();
    }

    @Test
    void rejectsOnlyUnderscores() {
        assertThat(valid("___")).isFalse();
    }
}
