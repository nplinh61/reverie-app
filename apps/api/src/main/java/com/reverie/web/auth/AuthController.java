package com.reverie.web.auth;

import com.reverie.account.EmailVerificationService;
import com.reverie.account.RegistrationCommand;
import com.reverie.account.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Account endpoints for the registration feature (API_DESIGN.md section 2).
 *
 * <p>All responses are intentionally neutral. Registration and resend always
 * return the same message whether or not the email already exists, so the API
 * never discloses account existence. Login, logout, and current-user are not
 * part of this feature and arrive in a later phase.
 *
 * <p>These endpoints are permitted for anonymous callers in
 * {@code SecurityConfig} but remain CSRF-protected, so the client must first
 * initialize the token with {@code GET /api/v1/auth/csrf}.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegistrationService registrationService;
    private final EmailVerificationService verificationService;

    public AuthController(RegistrationService registrationService,
            EmailVerificationService verificationService) {
        this.registrationService = registrationService;
        this.verificationService = verificationService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        registrationService.register(
                new RegistrationCommand(request.email(), request.password(),
                        request.displayName(), request.username()),
                ClientIp.from(http));
        return new MessageResponse(
                "If the details are valid, a verification email has been sent. Please check your inbox.");
    }

    @PostMapping("/verify-email")
    public MessageResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        verificationService.verify(request.token());
        return new MessageResponse("Your email address has been verified. You can now sign in.");
    }

    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponse resendVerification(@Valid @RequestBody ResendVerificationRequest request,
            HttpServletRequest http) {
        verificationService.resend(request.email(), ClientIp.from(http));
        return new MessageResponse(
                "If the details are valid, a verification email has been sent. Please check your inbox.");
    }
}
