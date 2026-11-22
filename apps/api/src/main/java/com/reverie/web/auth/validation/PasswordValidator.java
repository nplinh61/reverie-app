package com.reverie.web.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

/**
 * Enforces {@link Password}. Reports a distinct message for "too short" and
 * "too long" so the user knows which rule they hit, and measures the upper limit
 * in UTF-8 bytes rather than characters.
 */
public class PasswordValidator implements ConstraintValidator<Password, String> {

    private int minLength;
    private int maxBytes;

    @Override
    public void initialize(Password constraint) {
        this.minLength = constraint.minLength();
        this.maxBytes = constraint.maxBytes();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Null and blank are the concern of @NotBlank, not this constraint.
        if (value == null) {
            return true;
        }
        if (value.length() < minLength) {
            return fail(context, "must be at least " + minLength + " characters");
        }
        if (value.getBytes(StandardCharsets.UTF_8).length > maxBytes) {
            return fail(context, "must not exceed " + maxBytes + " bytes");
        }
        return true;
    }

    private boolean fail(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}
