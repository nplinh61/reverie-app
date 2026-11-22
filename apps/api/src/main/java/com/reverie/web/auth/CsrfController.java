package com.reverie.web.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoint that initializes the CSRF cookie before any mutating request
 * (ADR-003, SECURITY.md section 7).
 *
 * <p>Reading the token value forces Spring Security to generate it and write the
 * XSRF-TOKEN cookie on the response. The token itself is delivered only in the
 * cookie, so the body stays empty and the client echoes the value back as the
 * X-XSRF-TOKEN header on later writes such as registration and login.
 *
 * <p>The client calls this again after logout clears the cookie, and refreshes
 * its stored value from the cookie that login rotates. Those login and logout
 * flows arrive in Phase 4.
 */
@RestController
public class CsrfController {

    @GetMapping("/api/v1/auth/csrf")
    public ResponseEntity<Void> csrf(CsrfToken csrfToken) {
        csrfToken.getToken();
        return ResponseEntity.noContent().build();
    }
}
