package com.reverie.web.error;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

/**
 * Writes an {@link ApiError} body directly to a servlet response.
 *
 * <p>Spring Security's authentication entry point and access-denied handler run
 * in the filter chain, before the DispatcherServlet, so they cannot rely on the
 * normal message converters. This small helper keeps their JSON output identical
 * to the envelope produced by {@link GlobalExceptionHandler}, with a single
 * place to build it.
 */
final class ApiErrorResponseWriter {

    private ApiErrorResponseWriter() {
    }

    static void write(HttpServletResponse response, ObjectMapper objectMapper,
            HttpStatus status, String code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiError.of(status, code, message));
    }
}
