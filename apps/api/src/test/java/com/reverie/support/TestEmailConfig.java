package com.reverie.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Replaces the SMTP sender with a {@link RecordingEmailSender} in tests. Marked
 * {@link Primary} so it is injected wherever an {@code EmailSender} is required,
 * while the real SMTP bean still exists but is never used.
 */
@TestConfiguration
public class TestEmailConfig {

    @Bean
    @Primary
    RecordingEmailSender recordingEmailSender() {
        return new RecordingEmailSender();
    }
}
