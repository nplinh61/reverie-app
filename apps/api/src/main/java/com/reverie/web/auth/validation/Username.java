package com.reverie.web.auth.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates a username against the Milestone 1 policy (M1 spec section 3,
 * DATABASE_ERD.md section 3.2): 3 to 30 characters made of lowercase ASCII
 * letters, digits, and underscores, with at least one letter or digit. Leading
 * and trailing underscores are allowed.
 *
 * <p>Uppercase input is rejected rather than silently lowercased, so the stored
 * username always matches exactly what the policy permits. This constraint
 * ignores null and blank values so {@code @NotBlank} can own that message.
 */
@Documented
@Constraint(validatedBy = UsernameValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Username {

    String message() default "Invalid username.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
