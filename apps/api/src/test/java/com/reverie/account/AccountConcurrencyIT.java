package com.reverie.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.reverie.support.TestEmailConfig;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
 * Concurrency and persistence-state tests against a real PostgreSQL container.
 * These assert the invariants that the database constraints protect, so the
 * outcome is the same regardless of how the two threads actually interleave.
 */
@SpringBootTest
@Testcontainers
@Import(TestEmailConfig.class)
class AccountConcurrencyIT {

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

    @BeforeEach
    void reset() {
        jdbcTemplate.update("DELETE FROM email_verification_tokens");
        jdbcTemplate.update("DELETE FROM profiles");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void persistableTreatsALoadedAggregateAsAnUpdate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        userRepository.save(User.register("p@example.com", "{bcrypt}placeholder", now));

        // Loading sets isNew=false, so saving again is an UPDATE, not a second
        // INSERT that would violate the primary key.
        User loaded = userRepository.findByEmail("p@example.com").orElseThrow();
        userRepository.save(loaded);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM users WHERE email = ?", Integer.class, "p@example.com");
        assertThat(count).isEqualTo(1);
    }

    @Test
    void concurrentRegistrationWithSameEmailCreatesOneAccount() throws Exception {
        List<Throwable> outcomes = runBoth(
                () -> accountCreator.createAccount("race@example.com", "racer_one", "One", "correct horse battery"),
                () -> accountCreator.createAccount("race@example.com", "racer_two", "Two", "correct horse battery"));

        assertThat(successes(outcomes)).isEqualTo(1);
        assertThat(failure(outcomes)).isInstanceOf(EmailAlreadyRegisteredException.class);
        assertThat(countUsers("race@example.com")).isEqualTo(1);
    }

    @Test
    void concurrentRegistrationWithSameUsernameCreatesOneProfile() throws Exception {
        List<Throwable> outcomes = runBoth(
                () -> accountCreator.createAccount("one@example.com", "sameuser", "One", "correct horse battery"),
                () -> accountCreator.createAccount("two@example.com", "sameuser", "Two", "correct horse battery"));

        assertThat(successes(outcomes)).isEqualTo(1);
        assertThat(failure(outcomes)).isInstanceOf(UsernameAlreadyTakenException.class);

        Integer profiles = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM profiles WHERE lower(username) = 'sameuser'", Integer.class);
        assertThat(profiles).isEqualTo(1);
    }

    @Test
    void concurrentVerificationConsumesTheTokenExactlyOnce() throws Exception {
        IssuedVerification issued =
                accountCreator.createAccount("vic@example.com", "vic", "Vic", "correct horse battery");
        String rawToken = issued.rawToken();

        List<Throwable> outcomes = runBoth(
                () -> verificationService.verify(rawToken),
                () -> verificationService.verify(rawToken));

        assertThat(successes(outcomes)).isEqualTo(1);
        assertThat(failure(outcomes)).isInstanceOf(InvalidVerificationTokenException.class);

        UUID userId = userRepository.findByEmail("vic@example.com").orElseThrow().getId();
        assertThat(userRepository.findByEmail("vic@example.com").orElseThrow().isEmailVerified()).isTrue();
        Integer consumed = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM email_verification_tokens WHERE user_id = ? AND consumed_at IS NOT NULL",
                Integer.class, userId);
        assertThat(consumed).isEqualTo(1);
    }

    /** Runs two actions as simultaneously as possible; returns each one's thrown error or null. */
    private List<Throwable> runBoth(Runnable first, Runnable second) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Throwable> firstTask = () -> captureFailure(start, first);
        Callable<Throwable> secondTask = () -> captureFailure(start, second);
        try {
            Future<Throwable> firstResult = pool.submit(firstTask);
            Future<Throwable> secondResult = pool.submit(secondTask);
            start.countDown();
            return Arrays.asList(firstResult.get(10, TimeUnit.SECONDS), secondResult.get(10, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    private static Throwable captureFailure(CountDownLatch start, Runnable action) throws InterruptedException {
        start.await();
        try {
            action.run();
            return null;
        } catch (Throwable thrown) {
            return thrown;
        }
    }

    private static long successes(List<Throwable> outcomes) {
        return outcomes.stream().filter(Objects::isNull).count();
    }

    private static Throwable failure(List<Throwable> outcomes) {
        return outcomes.stream().filter(Objects::nonNull).findFirst().orElse(null);
    }

    private Integer countUsers(String email) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM users WHERE email = ?", Integer.class, email);
    }
}
