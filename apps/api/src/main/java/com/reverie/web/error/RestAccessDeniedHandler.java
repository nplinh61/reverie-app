package com.reverie.web.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Returns a 403 {@link ApiError} body when an authenticated request is denied,
 * and also when a CSRF check fails. The {@code CsrfFilter} reads its
 * access-denied handler from the same exception-handling configuration, so a
 * missing or invalid CSRF token produces this same envelope.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        ApiErrorResponseWriter.write(response, objectMapper, HttpStatus.FORBIDDEN,
                "FORBIDDEN", "Access is denied.");
    }
}
