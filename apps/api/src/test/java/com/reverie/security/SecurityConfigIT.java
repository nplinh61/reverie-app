package com.reverie.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Integration test for the Milestone 1 security foundation. It starts the full
 * application against a throwaway PostgreSQL 18 container and drives the real
 * Spring Security filter chain over HTTP, so it proves the actual behavior of
 * public access, the 401 and 403 envelopes, and the CSRF cookie flow rather than
 * assuming the configuration is correct.
 *
 * <p>The HTTP calls use the plain JDK {@link HttpClient}, which does not throw on
 * 4xx responses. For the CSRF flow a {@link CookieManager} stores the XSRF-TOKEN
 * cookie issued by the bootstrap endpoint and resends it automatically.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class SecurityConfigIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18"));

    @Value("${local.server.port}")
    private int port;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    @Test
    void pingRemainsPublic() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(uri("/api/v1/ping")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    void healthRemainsPublic() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(uri("/actuator/health")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    void protectedEndpointWithoutAuthReturns401Json() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(uri("/api/v1/secure-probe")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .contains("application/json");

        JsonNode body = objectMapper.readTree(response.body());
        assertThat(body.get("status").asInt()).isEqualTo(401);
        assertThat(body.get("code").asText()).isEqualTo("UNAUTHORIZED");
        assertThat(body.get("fieldErrors").isEmpty()).isTrue();
    }

    @Test
    void csrfBootstrapIssuesReadableCookie() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(uri("/api/v1/auth/csrf")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(204);

        List<String> setCookies = response.headers().allValues("set-cookie");
        assertThat(setCookies).anyMatch(cookie -> cookie.startsWith("XSRF-TOKEN="));
        // The cookie must be readable by JavaScript, so HttpOnly must be absent.
        assertThat(setCookies)
                .filteredOn(cookie -> cookie.startsWith("XSRF-TOKEN="))
                .allMatch(cookie -> !cookie.toLowerCase().contains("httponly"));
    }

    @Test
    void mutatingRequestWithoutCsrfTokenReturns403Json() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(uri("/api/v1/ping")).POST(BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .contains("application/json");

        JsonNode body = objectMapper.readTree(response.body());
        assertThat(body.get("status").asInt()).isEqualTo(403);
        assertThat(body.get("code").asText()).isEqualTo("FORBIDDEN");
    }

    @Test
    void mutatingRequestWithValidCsrfTokenPassesCsrfThenHitsAuth() throws Exception {
        CookieManager cookieManager = new CookieManager();
        HttpClient client = HttpClient.newBuilder().cookieHandler(cookieManager).build();

        HttpResponse<String> csrf = client.send(
                HttpRequest.newBuilder(uri("/api/v1/auth/csrf")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(csrf.statusCode()).isEqualTo(204);

        String token = cookieManager.getCookieStore().getCookies().stream()
                .filter(cookie -> cookie.getName().equals("XSRF-TOKEN"))
                .map(HttpCookie::getValue)
                .findFirst()
                .orElseThrow();

        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(uri("/api/v1/secure-probe"))
                        .header("X-XSRF-TOKEN", token)
                        .POST(BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        // The CSRF check passed (otherwise this would be 403). The protected
        // endpoint then rejects the still-unauthenticated request with 401.
        assertThat(response.statusCode()).isEqualTo(401);

        JsonNode body = objectMapper.readTree(response.body());
        assertThat(body.get("code").asText()).isEqualTo("UNAUTHORIZED");
    }
}
