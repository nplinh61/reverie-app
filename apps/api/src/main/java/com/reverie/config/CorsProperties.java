package com.reverie.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the explicit list of allowed browser origins from application.yml
 * (reverie.cors.allowed-origins). Keeping this in configuration lets us change
 * the frontend development origin without touching code.
 */
@ConfigurationProperties(prefix = "reverie.cors")
public class CorsProperties {

    private List<String> allowedOrigins = List.of();

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
}
