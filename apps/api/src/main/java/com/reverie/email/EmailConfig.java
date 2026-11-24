package com.reverie.email;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Activates {@link EmailProperties} binding for the email module. */
@Configuration
@EnableConfigurationProperties(EmailProperties.class)
public class EmailConfig {
}
