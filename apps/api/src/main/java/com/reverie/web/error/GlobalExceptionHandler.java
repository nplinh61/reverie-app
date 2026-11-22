package com.reverie.web.error;

import com.reverie.account.InvalidVerificationTokenException;
import com.reverie.account.UsernameAlreadyTakenException;
import com.reverie.ratelimit.RateLimitExceededException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns both expected framework errors and unexpected failures into one
 * consistent {@link ApiError} body.
 *
 * <p>It extends {@link ResponseEntityExceptionHandler} so Spring's own handling
 * of standard web exceptions (wrong method, unsupported media type, unknown
 * path, malformed JSON, validation) keeps its correct HTTP status. We only
 * replace the response body with our envelope; we never change the status. The
 * only status this class chooses itself is 500, and only for exceptions that
 * reach the catch-all below, which means they were genuinely unexpected.
 *
 * <p>Business conflicts carry their own exception types. A username already
 * being taken is reported as a 400 {@code VALIDATION_FAILED} with a field error
 * on {@code username}, because a username is a public handle rather than a
 * secret (see API_DESIGN.md section 2 and SECURITY.md section 2). It is handled
 * by {@link #handleUsernameAlreadyTaken} below, not treated as a generic
 * conflict. Rate-limit rejections map to 429 and invalid verification tokens to
 * a uniform 400, each with its own handler.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String VALIDATION_FAILED = "VALIDATION_FAILED";

    /**
     * Handles a {@code @Valid} request body failure. Here the binding result
     * carries real property paths, so each field error maps to a body property.
     * Object-level errors (constraints on the whole object) are reported under
     * the object name because they have no single property.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        List<ApiFieldError> fieldErrors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.add(new ApiFieldError(fieldError.getField(), messageOf(fieldError)));
        }
        for (ObjectError objectError : ex.getBindingResult().getGlobalErrors()) {
            fieldErrors.add(new ApiFieldError(objectError.getObjectName(), messageOf(objectError)));
        }

        ApiError body = ApiError.of(status, VALIDATION_FAILED, "Request validation failed.", fieldErrors);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    /**
     * Handles validation on individual method parameters, such as a constrained
     * request parameter or path variable.
     *
     * <p>Its structure is different from a request body. Each result belongs to
     * one method parameter. Only when that parameter is itself a validated
     * object does it carry field errors; a single value has no body property, so
     * the parameter name is the most accurate location we can report. We do not
     * assume every failure maps to a DTO field.
     */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        List<ApiFieldError> fieldErrors = new ArrayList<>();
        for (ParameterValidationResult result : ex.getParameterValidationResults()) {
            if (result instanceof ParameterErrors parameterErrors) {
                for (FieldError fieldError : parameterErrors.getFieldErrors()) {
                    fieldErrors.add(new ApiFieldError(fieldError.getField(), messageOf(fieldError)));
                }
            } else {
                String name = nameOf(result.getMethodParameter());
                for (MessageSourceResolvable error : result.getResolvableErrors()) {
                    fieldErrors.add(new ApiFieldError(name, messageOf(error)));
                }
            }
        }

        ApiError body = ApiError.of(status, VALIDATION_FAILED, "Request validation failed.", fieldErrors);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    /**
     * A taken username. Reported as a 400 field error on {@code username}, using
     * the same {@code VALIDATION_FAILED} shape as body validation so the client
     * handles it the same way. This is deliberately not neutral: a username is a
     * public handle, unlike an email address.
     */
    @ExceptionHandler(UsernameAlreadyTakenException.class)
    public ResponseEntity<ApiError> handleUsernameAlreadyTaken(UsernameAlreadyTakenException ex) {
        ApiError body = ApiError.of(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, "Request validation failed.",
                List.of(new ApiFieldError("username", "This username is already taken.")));
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * An unknown, expired, or already-consumed verification token. All three map
     * to one uniform response so the body never reveals which case applied.
     */
    @ExceptionHandler(InvalidVerificationTokenException.class)
    public ResponseEntity<ApiError> handleInvalidVerificationToken(InvalidVerificationTokenException ex) {
        ApiError body = ApiError.of(HttpStatus.BAD_REQUEST, "INVALID_VERIFICATION_TOKEN",
                "This verification link is invalid or has expired. Please request a new one.");
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * A rate-limit rejection. Returns 429 with a {@code Retry-After} header and a
     * generic body, and never says which bucket was hit.
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiError> handleRateLimitExceeded(RateLimitExceededException ex) {
        ApiError body = ApiError.of(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
                "Too many requests. Please try again later.");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(ex.getRetryAfterSeconds()))
                .body(body);
    }

    /**
     * Last-resort handler for exceptions that no more specific handler claims.
     * Reaching here means the failure was not an expected framework or
     * validation case, so it is reported as a generic 500. The real cause is
     * logged at error level and never placed in the response.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Unhandled exception while processing request", ex);
        ApiError body = ApiError.of(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred. Please try again later.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    /**
     * Central point where Spring's standard handlers render their response. The
     * incoming status is already correct for the exception, so we keep it and
     * only ensure the body is our envelope. When a handler above has already
     * built an {@link ApiError} (the validation cases), that body is kept as is.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {

        Object responseBody = (body instanceof ApiError)
                ? body
                : ApiError.of(statusCode, codeFor(statusCode), reasonFor(statusCode));
        return super.handleExceptionInternal(ex, responseBody, headers, statusCode, request);
    }

    /** Stable machine-readable code derived from the HTTP status, for example NOT_FOUND. */
    private static String codeFor(HttpStatusCode statusCode) {
        HttpStatus resolved = HttpStatus.resolve(statusCode.value());
        return resolved != null ? resolved.name() : "ERROR";
    }

    /** Safe human-readable summary from the standard HTTP reason phrase. */
    private static String reasonFor(HttpStatusCode statusCode) {
        HttpStatus resolved = HttpStatus.resolve(statusCode.value());
        return resolved != null ? resolved.getReasonPhrase() : "Request failed.";
    }

    /** Uses the resolved default message, with a plain fallback when none is set. */
    private static String messageOf(MessageSourceResolvable error) {
        String message = error.getDefaultMessage();
        return (message != null && !message.isBlank()) ? message : "Invalid value.";
    }

    /** Parameter name when available, falling back to the parameter type name. */
    private static String nameOf(MethodParameter parameter) {
        String name = parameter.getParameterName();
        return name != null ? name : parameter.getParameterType().getSimpleName();
    }
}
