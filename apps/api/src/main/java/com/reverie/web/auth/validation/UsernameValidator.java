package com.reverie.web.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Enforces {@link Username}. Checks the allowed characters and length first,
 * then the "at least one alphanumeric" rule, reporting a clear message for each.
 */
public class UsernameValidator implements ConstraintValidator<Username, String> {

    private static final Pattern ALLOWED = Pattern.compile("^[a-z0-9_]{3,30}$");
    private static final Pattern HAS_ALPHANUMERIC = Pattern.compile(".*[a-z0-9].*");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Null and blank are the concern of @NotBlank, not this constraint.
        if (value == null) {
            return true;
        }
        if (!ALLOWED.matcher(value).matches()) {
            return fail(context,
                    "must be 3 to 30 characters using lowercase letters, digits, and underscores");
        }
        if (!HAS_ALPHANUMERIC.matcher(value).matches()) {
            return fail(context, "must contain at least one letter or digit");
        }
        return true;
    }

    private boolean fail(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}
