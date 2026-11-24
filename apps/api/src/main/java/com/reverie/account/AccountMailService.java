package com.reverie.account;

import com.reverie.email.EmailMessage;
import com.reverie.email.EmailSender;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

/**
 * Builds the account-related emails and hands them to the {@link EmailSender}
 * transport. Keeping the content here means the transport stays generic and the
 * verification link is built in one place.
 *
 * <p>The verification link carries the raw token. That token is never logged;
 * only the composed message is passed to the transport.
 */
@Component
public class AccountMailService {

    private final EmailSender emailSender;
    private final VerificationProperties verificationProperties;

    public AccountMailService(EmailSender emailSender, VerificationProperties verificationProperties) {
        this.emailSender = emailSender;
        this.verificationProperties = verificationProperties;
    }

    /** Sends the verification link to a newly registered or resending user. */
    public void sendVerificationEmail(String to, String displayName, String rawToken) {
        String link = buildVerificationLink(rawToken);
        String subject = "Verify your Reverie email address";
        String body = """
                Hi %s,

                Welcome to Reverie. Please confirm your email address by opening this link:

                %s

                The link expires in 24 hours. If you did not create a Reverie account, you can
                safely ignore this email.
                """.formatted(displayName, link);
        emailSender.send(new EmailMessage(to, subject, body));
    }

    /**
     * Sent instead of a verification email when someone tries to register an
     * email that already has an account. It lets the real owner know, without
     * ever telling the registrant that the address exists.
     */
    public void sendAccountExistsNotice(String to) {
        String subject = "About your Reverie account";
        String body = """
                Hi,

                Someone just tried to create a Reverie account with this email address, but an
                account already exists for it.

                If this was you, there is nothing to do: you can simply log in. If you did not do
                this, you can ignore this email. No new account was created.
                """;
        emailSender.send(new EmailMessage(to, subject, body));
    }

    private String buildVerificationLink(String rawToken) {
        String base = verificationProperties.getBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/verify-email?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }
}
