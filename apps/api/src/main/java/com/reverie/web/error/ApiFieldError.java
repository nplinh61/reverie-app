package com.reverie.web.error;

/**
 * One field-level validation problem inside an {@link ApiError}.
 *
 * <p>The {@code field} names where the problem is. For a request body it is the
 * property path, such as {@code email}. For a single request parameter or path
 * variable it is the parameter name, because there is no body property to point
 * at. The {@code message} is the human-readable reason.
 */
public record ApiFieldError(String field, String message) {
}
