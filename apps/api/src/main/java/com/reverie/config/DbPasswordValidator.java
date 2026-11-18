package com.reverie.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Fails fast when the required DB_PASSWORD is missing or blank.
 *
 * <p>This runs after the environment is prepared but before the application
 * context, datasource, and Flyway are created, so the application stops with a
 * clear, actionable message instead of a later database authentication failure.
 */
public class DbPasswordValidator implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String password = environment.getProperty("DB_PASSWORD");
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "DB_PASSWORD is required but was missing or blank. "
                    + "Set the DB_PASSWORD environment variable (see .env.example) "
                    + "before starting the application.");
        }
    }
}
