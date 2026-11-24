package com.reverie.support;

import com.reverie.email.EmailMessage;
import com.reverie.email.EmailSender;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A deterministic {@link EmailSender} for tests. It records every message in
 * memory instead of sending over SMTP, so tests can assert what would have been
 * sent and can read the raw verification token out of the body. Real SMTP
 * delivery to Mailpit is checked separately during local end-to-end testing
 * (decision D).
 */
public class RecordingEmailSender implements EmailSender {

    private final List<EmailMessage> sent = new CopyOnWriteArrayList<>();
    private volatile boolean failing = false;

    @Override
    public void send(EmailMessage message) {
        if (failing) {
            // Simulates an SMTP transport failure, to prove it does not break a
            // registration whose account is already committed.
            throw new IllegalStateException("simulated SMTP failure");
        }
        sent.add(message);
    }

    public List<EmailMessage> sent() {
        return List.copyOf(sent);
    }

    /** When set, the next sends throw, as a down SMTP server would. */
    public void setFailing(boolean failing) {
        this.failing = failing;
    }

    public void clear() {
        sent.clear();
        failing = false;
    }

    /** The most recent message addressed to {@code to}, if any. */
    public Optional<EmailMessage> lastTo(String to) {
        return sent.stream()
                .filter(message -> message.to().equals(to))
                .reduce((first, second) -> second);
    }
}
