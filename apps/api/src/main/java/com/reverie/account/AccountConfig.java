package com.reverie.account;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring for the account module.
 *
 * <p>Provides a single UTC {@link Clock} so services read time from one
 * injectable source. Tests can replace it with a fixed clock to exercise token
 * expiry and the resend cooldown deterministically.
 */
@Configuration
@EnableConfigurationProperties(VerificationProperties.class)
public class AccountConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
