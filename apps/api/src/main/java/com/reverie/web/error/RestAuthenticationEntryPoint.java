package com.reverie.web.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Returns a 401 {@link ApiError} body when an unauthenticated request reaches a
 * protected endpoint. Replaces Spring Security's default behavior, which would
 * otherwise redirect to a login page or send an HTTP Basic challenge.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authenticationException) throws IOException {
        ApiErrorResponseWriter.write(response, objectMapper, HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED", "Authentication is required to access this resource.");
    }
}
