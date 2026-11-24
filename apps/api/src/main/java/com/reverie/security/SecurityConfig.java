package com.reverie.security;

import com.reverie.config.CorsProperties;
import com.reverie.web.error.RestAccessDeniedHandler;
import com.reverie.web.error.RestAuthenticationEntryPoint;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Central security configuration for the API (ADR-003).
 *
 * <p>This is the Milestone 1 security foundation. It locks every endpoint by
 * default, keeps only the liveness, health, and CSRF-bootstrap endpoints public,
 * and returns the shared {@link com.reverie.web.error.ApiError} body for both
 * 401 and 403 responses. Login, logout, a user store, and session hardening
 * (cookie flags and the two-hour timeout) arrive in Phase 4; nothing here
 * authenticates a real user yet.
 *
 * <p>Authentication uses cookie sessions, so CSRF protection is enabled for all
 * unsafe methods and is never globally disabled. The CSRF token is stored in a
 * cookie that JavaScript can read (REVERIE-XSRF-TOKEN) and echoed back as the
 * X-REVERIE-XSRF-TOKEN header. The names are project-specific so the cookie
 * never collides with another app that shares the same host during local
 * development, because cookies are not isolated by port. The public
 * {@code GET /api/v1/auth/csrf} endpoint issues the cookie before the first
 * mutating request.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

    private final CorsProperties corsProperties;

    public SecurityConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler,
            @Value("${server.servlet.session.cookie.name}") String sessionCookieName) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(reverieCsrfTokenRepository())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/v1/ping").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        // Account entry points are reachable before authentication.
                        // They stay CSRF-protected, so the client bootstraps the
                        // token through GET /api/v1/auth/csrf first.
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/verify-email").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/resend-verification").permitAll()
                        .anyRequest().authenticated())
                // No form login or HTTP Basic: an unauthenticated request must get a
                // 401 JSON body, not a login-page redirect or a browser Basic prompt.
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(logout -> logout.deleteCookies(sessionCookieName))
                // Wiring the handlers here covers both the ExceptionTranslationFilter
                // (401 and action 403) and the CsrfFilter, which reads its
                // access-denied handler from this same configurer, so CSRF failures
                // also return the ApiError body.
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }

    /**
     * CSRF token repository with project-specific cookie and header names.
     *
     * <p>The default {@code XSRF-TOKEN} cookie is shared by every Angular and
     * Spring Security app on the same host, and cookies are not isolated by port.
     * Running two such apps on {@code localhost} therefore lets their CSRF cookies
     * collide, which can make the header and cookie disagree and fail the check.
     * Naming this cookie {@code REVERIE-XSRF-TOKEN} and its header
     * {@code X-REVERIE-XSRF-TOKEN} keeps Reverie's token in its own namespace. The
     * repository stays stateless and HttpOnly-false so the Angular client can read
     * the cookie and echo it back as the header.
     */
    private static CookieCsrfTokenRepository reverieCsrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieName("REVERIE-XSRF-TOKEN");
        repository.setHeaderName("X-REVERIE-XSRF-TOKEN");
        return repository;
    }

    /**
     * Password hashing (ADR-003). A {@link DelegatingPasswordEncoder} stores an
     * algorithm prefix such as {@code {bcrypt}} alongside each hash, so the
     * algorithm can be upgraded later without breaking existing hashes. BCrypt at
     * cost 12 is the current default; the cost is benchmarked before production.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        String encodingId = "bcrypt";
        Map<String, PasswordEncoder> encoders = Map.of(encodingId, new BCryptPasswordEncoder(12));
        return new DelegatingPasswordEncoder(encodingId, encoders);
    }

    /**
     * CORS policy, now owned by Spring Security instead of an MVC configurer. The
     * allowed origins come from {@code reverie.cors.allowed-origins}.
     *
     * <p>allowCredentials is intentionally left at its default of false. Browser
     * cross-origin credentialed requests are not supported yet. In development the
     * Angular dev-server proxy (Step 5) makes browser traffic same-origin, and in
     * production a reverse proxy serves the API under one domain, so credentialed
     * cross-origin requests are not on the normal path. X-REVERIE-XSRF-TOKEN is
     * allowed so that any permitted cross-origin client can still carry the CSRF
     * token.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "Authorization", "Accept", "X-REVERIE-XSRF-TOKEN"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/**", configuration);
        return source;
    }
}
