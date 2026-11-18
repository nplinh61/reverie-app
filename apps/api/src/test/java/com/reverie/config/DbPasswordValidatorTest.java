package com.reverie.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * Unit test for {@link DbPasswordValidator}. It needs no Spring context: the
 * validator is a plain object, so the test builds an environment directly and
 * calls the method. The {@code SpringApplication} argument is unused by the
 * validator, so {@code null} is passed on purpose.
 */
class DbPasswordValidatorTest {

    private final DbPasswordValidator validator = new DbPasswordValidator();

    @Test
    void acceptsNonBlankPassword() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("DB_PASSWORD", "a-password");

        assertThatCode(() -> validator.postProcessEnvironment(environment, null))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingPassword() {
        MockEnvironment environment = new MockEnvironment();

        assertThatThrownBy(() -> validator.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DB_PASSWORD");
    }

    @Test
    void rejectsBlankPassword() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("DB_PASSWORD", "   ");

        assertThatThrownBy(() -> validator.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DB_PASSWORD");
    }
}
