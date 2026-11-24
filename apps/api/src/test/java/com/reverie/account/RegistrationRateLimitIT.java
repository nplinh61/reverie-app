package com.reverie.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.reverie.support.TestEmailConfig;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Verifies the registration rate limit end to end with a deliberately tiny limit
 * (2 per IP). The third attempt from the same client is rejected with 429 and a
 * Retry-After header, and never discloses which account state applied.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "reverie.rate-limit.register-per-ip.max=2",
            "reverie.rate-limit.register-per-ip.window=1h"
        })
@Testcontainers
@Import(TestEmailConfig.class)
class RegistrationRateLimitIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18"));

    @Value("${local.server.port}")
    private int port;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void rejectsTheThirdRegistrationFromTheSameClientWith429() throws Exception {
        assertThat(register("a@example.com", "aaa").statusCode()).isEqualTo(202);
        assertThat(register("b@example.com", "bbb").statusCode()).isEqualTo(202);

        HttpResponse<String> third = register("c@example.com", "ccc");
        assertThat(third.statusCode()).isEqualTo(429);
        assertThat(third.headers().firstValue("Retry-After")).isPresent();
        assertThat(objectMapper.readTree(third.body()).get("code").asText()).isEqualTo("RATE_LIMITED");
    }

    private HttpResponse<String> register(String email, String username) throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "email", email, "password", "correct horse battery",
                "displayName", "Tester", "username", username));

        CookieManager cookieManager = new CookieManager();
        HttpClient client = HttpClient.newBuilder().cookieHandler(cookieManager).build();
        client.send(HttpRequest.newBuilder(uri("/api/v1/auth/csrf")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        String csrfToken = cookieManager.getCookieStore().getCookies().stream()
                .filter(cookie -> cookie.getName().equals("REVERIE-XSRF-TOKEN"))
                .map(HttpCookie::getValue)
                .findFirst()
                .orElseThrow();

        return client.send(HttpRequest.newBuilder(uri("/api/v1/auth/register"))
                .header("X-REVERIE-XSRF-TOKEN", csrfToken)
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(json))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
