package com.reverie.web.auth.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates a raw password against the Milestone 1 policy (ADR-003,
 * SECURITY.md section 2): at least {@code minLength} characters and at most
 * {@code maxBytes} UTF-8 bytes.
 *
 * <p>The upper bound is a byte limit, not a character count, because BCrypt
 * operates on 72 bytes and silently truncates anything longer. Rejecting
 * over-limit input, rather than truncating it, keeps the stored hash faithful to
 * what the user typed. This constraint ignores null and blank values so
 * {@code @NotBlank} can own that message.
 */
@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Password {

    String message() default "Invalid password.";

    int minLength() default 10;

    int maxBytes() default 72;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
