package com.reverie.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Sender identity bound from {@code reverie.mail}. Applied by
 * {@link SmtpEmailSender} so every message has a consistent From.
 */
@ConfigurationProperties(prefix = "reverie.mail")
public class EmailProperties {

    private String fromAddress = "no-reply@reverie.local";
    private String fromName = "Reverie";

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }
}
