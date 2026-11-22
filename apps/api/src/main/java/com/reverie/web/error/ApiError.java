package com.reverie.web.error;

import java.util.List;
import org.springframework.http.HttpStatusCode;

/**
 * The single error body shape returned for every failed API request.
 *
 * <p>The shape is intentionally stable and small. {@code status} is the HTTP
 * status code, {@code code} is a stable machine-readable name for the error,
 * and {@code message} is a short human-readable summary. {@code fieldErrors}
 * is empty for everything except validation failures, so clients can always
 * read the same four fields.
 *
 * <p>The body never carries exception types, stack traces, SQL, or any other
 * internal detail. Those belong only in server logs (see SECURITY.md section 7).
 */
public record ApiError(int status, String code, String message, List<ApiFieldError> fieldErrors) {

    /** Builds an error with no field-level detail. */
    public static ApiError of(HttpStatusCode status, String code, String message) {
        return new ApiError(status.value(), code, message, List.of());
    }

    /** Builds a validation error that carries one or more field-level problems. */
    public static ApiError of(HttpStatusCode status, String code, String message,
                              List<ApiFieldError> fieldErrors) {
        return new ApiError(status.value(), code, message, List.copyOf(fieldErrors));
    }
}
