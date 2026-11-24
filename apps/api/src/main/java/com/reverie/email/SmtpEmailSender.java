package com.reverie.email;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * SMTP implementation of {@link EmailSender} (ADR-005). It uses Spring's
 * {@link JavaMailSender}, which Spring Boot auto-configures from the
 * {@code spring.mail.*} settings. Locally that points at the Mailpit container,
 * which captures the mail for inspection and forwards nothing.
 */
@Component
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final EmailProperties properties;

    public SmtpEmailSender(JavaMailSender mailSender, EmailProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void send(EmailMessage message) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(properties.getFromName() + " <" + properties.getFromAddress() + ">");
        mail.setTo(message.to());
        mail.setSubject(message.subject());
        mail.setText(message.body());
        mailSender.send(mail);
    }
}
