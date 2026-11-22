package com.reverie.account;

import com.reverie.ratelimit.RateLimitGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestrates registration (API_DESIGN.md section 2, ADR-005). It is not
 * transactional itself: it applies the rate limit, delegates the user, profile,
 * and token writes to {@link AccountCreator} in a single transaction, and then
 * sends email only after that transaction commits, so a rollback never leaves an
 * email promising a token that was not stored.
 *
 * <p>The response to the caller is always neutral. A brand-new email receives a
 * verification link; an already-registered email receives an "account exists"
 * notice and no second account. A taken username is the one non-neutral case: it
 * surfaces as {@link UsernameAlreadyTakenException}, because a username is a
 * public handle rather than a secret.
 *
 * <p>Email delivery is best-effort. Once the account is committed, a later SMTP
 * failure must not turn a successful registration into an error: it is logged and
 * swallowed, and the user recovers by requesting a resend. The log line carries no
 * token or password; the raw token lives only in the message body, which is never
 * part of a mail-transport exception.
 */
@Service
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    private final AccountCreator accountCreator;
    private final AccountMailService mailService;
    private final RateLimitGuard rateLimitGuard;

    public RegistrationService(AccountCreator accountCreator, AccountMailService mailService,
            RateLimitGuard rateLimitGuard) {
        this.accountCreator = accountCreator;
        this.mailService = mailService;
        this.rateLimitGuard = rateLimitGuard;
    }

    public void register(RegistrationCommand command, String clientIp) {
        rateLimitGuard.checkRegistration(clientIp);

        String email = EmailNormalizer.normalize(command.email());
        // The username passed validation, so it is already lowercase ASCII.
        String username = command.username();

        IssuedVerification issued;
        try {
            issued = accountCreator.createAccount(email, username, command.displayName(), command.password());
        } catch (EmailAlreadyRegisteredException ex) {
            // Neutral duplicate-email path: no second account, notify the real owner.
            sendQuietly("account-exists notice", () -> mailService.sendAccountExistsNotice(email));
            return;
        }
        // UsernameAlreadyTakenException is thrown before commit and intentionally
        // propagates to the web layer as a 400 field error.

        // The account is now committed. Email is best-effort from here on.
        sendQuietly("verification email", () ->
                mailService.sendVerificationEmail(issued.email(), issued.displayName(), issued.rawToken()));
    }

    /**
     * Sends an email, turning any failure into a logged warning rather than a
     * failed registration. The account already exists at this point, so resend is
     * the recovery path. No sensitive data is logged.
     */
    private void sendQuietly(String what, Runnable send) {
        try {
            send.run();
        } catch (RuntimeException ex) {
            log.warn("Failed to send {} during registration; the account exists and "
                    + "the user can request a resend", what, ex);
        }
    }
}
