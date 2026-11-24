package com.reverie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

/**
 * Entry point for the Reverie backend API.
 *
 * <p>The security configuration lives in {@code com.reverie.security}. The
 * default generated in-memory user is excluded on purpose: Reverie manages its
 * own authentication from Milestone 1, Phase 4 onwards, so Spring Boot must not
 * create a throwaway user or log a generated password at startup.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class ReverieApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReverieApplication.class, args);
    }
}
