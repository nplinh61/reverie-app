package com.reverie.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.reverie.email.EmailMessage;
import com.reverie.support.RecordingEmailSender;
import com.reverie.support.TestEmailConfig;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * End-to-end registration and verification over real HTTP against a throwaway
 * PostgreSQL container. It drives the full stack: Spring Security and CSRF, the
 * controller, validation, the services, and persistence. Email is captured by a
 * {@link RecordingEmailSender} so the test can read the verification token out of
 * the message, exactly as a user would from their inbox.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            // The limiter is a shared singleton and every test call comes from
            // localhost, so raise the limits well above what these functional
            // tests need. The limiter's own behavior is covered by
            // RegistrationRateLimitIT and InMemoryRateLimiterTest.
            "reverie.rate-limit.register-per-ip.max=1000",
            "reverie.rate-limit.resend-per-ip.max=1000",
            "reverie.rate-limit.resend-per-email.max=1000"
        })
@Testcontainers
@Import(TestEmailConfig.class)
class RegistrationIT {

    private static final Pattern TOKEN_IN_LINK = Pattern.compile("token=([A-Za-z0-9_-]+)");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18"));

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private RecordingEmailSender emailSender;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @BeforeEach
    void reset() {
        emailSender.clear();
        // Foreign-key safe order.
        jdbcTemplate.update("DELETE FROM email_verification_tokens");
        jdbcTemplate.update("DELETE FROM profiles");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void registersUserProfileAndTokenAndSendsVerificationEmail() throws Exception {
        HttpResponse<String> response = register("alice@example.com", "correct horse battery", "Alice", "alice");

        assertThat(response.statusCode()).isEqualTo(202);
        // The raw token must never appear in the response body.
        assertThat(response.body()).doesNotContain("token");

        Optional<User> user = userRepository.findByEmail("alice@example.com");
        assertThat(user).isPresent();
        assertThat(user.get().isEmailVerified()).isFalse();
        assertThat(user.get().getPasswordHash()).startsWith("{bcrypt}");
        assertThat(user.get().getPasswordHash()).doesNotContain("correct horse battery");

        assertThat(profileRepository.findByUserId(user.get().getId())).isPresent();

        Integer tokenCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM email_verification_tokens WHERE user_id = ?",
                Integer.class, user.get().getId());
        assertThat(tokenCount).isEqualTo(1);

        EmailMessage mail = emailSender.lastTo("alice@example.com").orElseThrow();
        assertThat(mail.body()).contains("/verify-email?token=");
    }

    @Test
    void registrationStaysNeutral202WhenEmailDeliveryFails() throws Exception {
        emailSender.setFailing(true);

        HttpResponse<String> response =
                register("fail@example.com", "correct horse battery", "Fail", "failuser");

        // The account was committed before the send attempt, so the SMTP failure
        // is swallowed and the response is the normal neutral 202.
        assertThat(response.statusCode()).isEqualTo(202);
        assertThat(userRepository.findByEmail("fail@example.com")).isPresent();
    }

    @Test
    void normalizesEmailBeforeStoring() throws Exception {
        register("Alice@Example.COM", "correct horse battery", "Alice", "alice");

        assertThat(userRepository.findByEmail("alice@example.com")).isPresent();
    }

    @Test
    void duplicateEmailIsNeutralAndCreatesNoSecondAccount() throws Exception {
        register("alice@example.com", "correct horse battery", "Alice", "alice");
        emailSender.clear();

        HttpResponse<String> second =
                register("alice@example.com", "another password", "Alice Two", "alicetwo");

        // Same neutral status as a real registration.
        assertThat(second.statusCode()).isEqualTo(202);

        Integer userCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM users WHERE email = ?", Integer.class, "alice@example.com");
        assertThat(userCount).isEqualTo(1);
        // The second username was never created.
        assertThat(profileRepository.existsByUsernameIgnoreCase("alicetwo")).isFalse();

        // An account-exists notice is sent, not a verification link.
        EmailMessage mail = emailSender.lastTo("alice@example.com").orElseThrow();
        assertThat(mail.body()).doesNotContain("/verify-email?token=");
        assertThat(mail.subject()).containsIgnoringCase("account");
    }

    @Test
    void takenUsernameReturnsFieldErrorAndNoAccount() throws Exception {
        register("alice@example.com", "correct horse battery", "Alice", "alice");

        HttpResponse<String> conflict =
                register("bob@example.com", "correct horse battery", "Bob", "alice");

        assertThat(conflict.statusCode()).isEqualTo(400);
        JsonNode body = objectMapper.readTree(conflict.body());
        assertThat(body.get("code").asText()).isEqualTo("VALIDATION_FAILED");
        assertThat(hasFieldError(body, "username")).isTrue();

        assertThat(userRepository.findByEmail("bob@example.com")).isEmpty();
    }

    @Test
    void shortPasswordIsRejectedAndNoAccount() throws Exception {
        HttpResponse<String> response =
                register("alice@example.com", "short", "Alice", "alice");

        assertThat(response.statusCode()).isEqualTo(400);
        JsonNode body = objectMapper.readTree(response.body());
        assertThat(body.get("code").asText()).isEqualTo("VALIDATION_FAILED");
        assertThat(hasFieldError(body, "password")).isTrue();
        assertThat(userRepository.findByEmail("alice@example.com")).isEmpty();
    }

    @Test
    void verifiesEmailFromTheLinkTokenThenRejectsReuse() throws Exception {
        register("alice@example.com", "correct horse battery", "Alice", "alice");
        String token = extractToken(emailSender.lastTo("alice@example.com").orElseThrow().body());

        HttpResponse<String> verify = postWithCsrf("/api/v1/auth/verify-email",
                objectMapper.writeValueAsString(Map.of("token", token)));

        assertThat(verify.statusCode()).isEqualTo(200);
        assertThat(userRepository.findByEmail("alice@example.com").orElseThrow().isEmailVerified()).isTrue();

        // The same token cannot be used twice.
        HttpResponse<String> reuse = postWithCsrf("/api/v1/auth/verify-email",
                objectMapper.writeValueAsString(Map.of("token", token)));
        assertThat(reuse.statusCode()).isEqualTo(400);
        JsonNode body = objectMapper.readTree(reuse.body());
        assertThat(body.get("code").asText()).isEqualTo("INVALID_VERIFICATION_TOKEN");
    }

    @Test
    void unknownVerificationTokenIsUniform400() throws Exception {
        HttpResponse<String> verify = postWithCsrf("/api/v1/auth/verify-email",
                objectMapper.writeValueAsString(Map.of("token", "not-a-real-token")));

        assertThat(verify.statusCode()).isEqualTo(400);
        JsonNode body = objectMapper.readTree(verify.body());
        assertThat(body.get("code").asText()).isEqualTo("INVALID_VERIFICATION_TOKEN");
    }

    @Test
    void resendForUnknownEmailIsNeutralAndSendsNothing() throws Exception {
        HttpResponse<String> resend = postWithCsrf("/api/v1/auth/resend-verification",
                objectMapper.writeValueAsString(Map.of("email", "nobody@example.com")));

        assertThat(resend.statusCode()).isEqualTo(202);
        assertThat(emailSender.lastTo("nobody@example.com")).isEmpty();
    }

    @Test
    void resendWithinCooldownSendsNoNewEmail() throws Exception {
        register("carol@example.com", "correct horse battery", "Carol", "carol");
        emailSender.clear();

        // The default 60-second cooldown has not elapsed, so this resend is a no-op.
        HttpResponse<String> resend = postWithCsrf("/api/v1/auth/resend-verification",
                objectMapper.writeValueAsString(Map.of("email", "carol@example.com")));

        assertThat(resend.statusCode()).isEqualTo(202);
        assertThat(emailSender.lastTo("carol@example.com")).isEmpty();

        var user = userRepository.findByEmail("carol@example.com").orElseThrow();
        Integer tokenCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM email_verification_tokens WHERE user_id = ?",
                Integer.class, user.getId());
        assertThat(tokenCount).isEqualTo(1);
    }

    private HttpResponse<String> register(String email, String password, String displayName,
            String username) throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "email", email, "password", password, "displayName", displayName, "username", username));
        return postWithCsrf("/api/v1/auth/register", json);
    }

    /** Bootstraps the CSRF cookie, then sends the POST carrying the matching header. */
    private HttpResponse<String> postWithCsrf(String path, String json) throws Exception {
        CookieManager cookieManager = new CookieManager();
        HttpClient client = HttpClient.newBuilder().cookieHandler(cookieManager).build();

        client.send(HttpRequest.newBuilder(uri("/api/v1/auth/csrf")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        String csrfToken = cookieManager.getCookieStore().getCookies().stream()
                .filter(cookie -> cookie.getName().equals("XSRF-TOKEN"))
                .map(HttpCookie::getValue)
                .findFirst()
                .orElseThrow();

        return client.send(HttpRequest.newBuilder(uri(path))
                .header("X-XSRF-TOKEN", csrfToken)
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(json))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private static boolean hasFieldError(JsonNode body, String field) {
        for (JsonNode fieldError : body.get("fieldErrors")) {
            if (field.equals(fieldError.get("field").asText())) {
                return true;
            }
        }
        return false;
    }

    private static String extractToken(String emailBody) {
        Matcher matcher = TOKEN_IN_LINK.matcher(emailBody);
        if (!matcher.find()) {
            throw new IllegalStateException("No verification token found in email body");
        }
        return matcher.group(1);
    }
}
