package com.reverie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Reverie backend API.
 *
 * <p>This is the Milestone 0 skeleton. It only starts a Spring Boot application
 * context. Datasource configuration, database migrations, and web endpoints are
 * added in later Milestone 0 phases.
 */
@SpringBootApplication
public class ReverieApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReverieApplication.class, args);
    }
}
