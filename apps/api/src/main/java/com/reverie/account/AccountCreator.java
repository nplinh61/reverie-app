package com.reverie.account;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the user, profile, and first verification token in one transaction
 * (behavioral invariant 1). It lives in its own bean so the transaction boundary
 * is a real Spring proxy call from the orchestrating {@link RegistrationService},
 * and so the verification email can be sent only after this transaction commits.
 *
 * <p>Application pre-checks avoid the common duplicate case, but the PostgreSQL
 * unique constraints are authoritative under a race (API_DESIGN.md section 2). A
 * violation that slips past the pre-check is caught and mapped back to the same
 * two outcomes by inspecting which constraint failed: a username conflict becomes
 * a field error, a duplicate email becomes the neutral path. Any other integrity
 * error is rethrown and surfaces as a generic 500, never silently swallowed.
 */
@Component
public class AccountCreator {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenFactory tokenFactory;
    private final VerificationProperties verificationProperties;
    private final java.time.Clock clock;

    public AccountCreator(UserRepository userRepository, ProfileRepository profileRepository,
            EmailVerificationTokenRepository tokenRepository, PasswordEncoder passwordEncoder,
            VerificationTokenFactory tokenFactory, VerificationProperties verificationProperties,
            java.time.Clock clock) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenFactory = tokenFactory;
        this.verificationProperties = verificationProperties;
        this.clock = clock;
    }

    @Transactional
    public IssuedVerification createAccount(String normalizedEmail, String username,
            String displayName, String rawPassword) {

        if (profileRepository.existsByUsernameIgnoreCase(username)) {
            throw new UsernameAlreadyTakenException();
        }
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new EmailAlreadyRegisteredException();
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        String passwordHash = passwordEncoder.encode(rawPassword);
        User user = User.register(normalizedEmail, passwordHash, now);
        try {
            userRepository.save(user);
            profileRepository.save(Profile.create(user.getId(), username, displayName, null, now));
        } catch (DbActionExecutionException | DataIntegrityViolationException ex) {
            throw mapDuplicate(ex);
        }

        String rawToken = tokenFactory.generateRawToken();
        tokenRepository.save(EmailVerificationToken.issue(
                user.getId(), tokenFactory.hash(rawToken), now, verificationProperties.getTokenValidity()));

        return new IssuedVerification(normalizedEmail, displayName, rawToken);
    }

    /**
     * Maps a unique-constraint violation to the right domain exception by the
     * constraint name. If it is neither the email nor the username constraint, the
     * original exception is returned so the failure is not hidden.
     */
    private RuntimeException mapDuplicate(RuntimeException ex) {
        String constraint = extractConstraintName(ex);
        if (constraint != null) {
            String name = constraint.toLowerCase(Locale.ROOT);
            if (name.contains("username")) {
                return new UsernameAlreadyTakenException();
            }
            if (name.contains("email")) {
                return new EmailAlreadyRegisteredException();
            }
        }
        return ex;
    }

    /**
     * Reads the violated constraint name from the exception message, for example
     * {@code violates unique constraint "uq_users_email"}. Parsing the message
     * keeps the PostgreSQL driver at runtime scope instead of pulling its classes
     * onto the compile classpath just to read one field.
     */
    private static String extractConstraintName(Throwable ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null) {
                Matcher matcher = CONSTRAINT_PATTERN.matcher(message);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        }
        return null;
    }

    private static final Pattern CONSTRAINT_PATTERN = Pattern.compile("constraint \"([^\"]+)\"");
}
