package com.reverie.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.reverie.support.RecordingEmailSender;
import com.reverie.support.TestEmailConfig;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Resend behavior with the cooldown disabled, so the test can exercise the
 * invalidation path directly. It checks that a resend invalidates the previous
 * unconsumed token and issues exactly one fresh one (ADR-005). The 60-second
 * cooldown itself is covered by RegistrationIT.
 */
@SpringBootTest(properties = "reverie.verification.resend-cooldown=0s")
@Testcontainers
@Import(TestEmailConfig.class)
class ResendInvalidationIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18"));

    @Autowired
    private AccountCreator accountCreator;

    @Autowired
    private EmailVerificationService verificationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RecordingEmailSender emailSender;

    @BeforeEach
    void reset() {
        emailSender.clear();
        jdbcTemplate.update("DELETE FROM email_verification_tokens");
        jdbcTemplate.update("DELETE FROM profiles");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void resendInvalidatesPriorTokenAndIssuesExactlyOneNew() {
        IssuedVerification first =
                accountCreator.createAccount("dave@example.com", "dave", "Dave", "correct horse battery");
        UUID userId = userRepository.findByEmail("dave@example.com").orElseThrow().getId();
        assertThat(unconsumedCount(userId)).isEqualTo(1);
        emailSender.clear();

        verificationService.resend("dave@example.com", "127.0.0.1");

        // A verification email went out again.
        assertThat(emailSender.lastTo("dave@example.com")).isPresent();
        // Two tokens exist in total, but only the newest is still usable.
        assertThat(totalCount(userId)).isEqualTo(2);
        assertThat(unconsumedCount(userId)).isEqualTo(1);
        // The original token was invalidated and no longer verifies.
        assertThatThrownBy(() -> verificationService.verify(first.rawToken()))
                .isInstanceOf(InvalidVerificationTokenException.class);
    }

    private Integer unconsumedCount(UUID userId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM email_verification_tokens WHERE user_id = ? AND consumed_at IS NULL",
                Integer.class, userId);
    }

    private Integer totalCount(UUID userId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM email_verification_tokens WHERE user_id = ?",
                Integer.class, userId);
    }
}
