package com.reverie.web.error;

import com.reverie.account.InvalidVerificationTokenException;
import com.reverie.account.UsernameAlreadyTakenException;
import com.reverie.ratelimit.RateLimitExceededException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only controller that exists solely to trigger error branches of
 * {@link GlobalExceptionHandler}. It lives in the test sources and is never
 * shipped. The request body uses {@code @Valid}, which produces a
 * {@code MethodArgumentNotValidException} when invalid.
 *
 * <p>Method-parameter validation ({@code HandlerMethodValidationException}) is
 * handled by the advice but is not exercised here. Its test is deferred to a
 * later phase that runs against a full application context, because the
 * dependency-free standalone setup used by these web tests does not enable
 * controller method-parameter validation.
 */
@RestController
class ErrorTestController {

    @GetMapping("/test/ok")
    String ok() {
        return "ok";
    }

    @PostMapping("/test/body")
    String body(@Valid @RequestBody SampleRequest request) {
        return request.name();
    }

    @GetMapping("/test/boom")
    String boom() {
        throw new IllegalStateException("internal detail that must not leak");
    }

    @GetMapping("/test/username-taken")
    String usernameTaken() {
        throw new UsernameAlreadyTakenException();
    }

    @GetMapping("/test/invalid-token")
    String invalidToken() {
        throw new InvalidVerificationTokenException();
    }

    @GetMapping("/test/rate-limited")
    String rateLimited() {
        throw new RateLimitExceededException(42);
    }

    record SampleRequest(@NotBlank String name, @Email String email) {
    }
}
