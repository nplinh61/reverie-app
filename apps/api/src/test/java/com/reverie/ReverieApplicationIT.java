package com.reverie;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Integration test for the Milestone 0 foundation. It starts the full
 * application against a throwaway PostgreSQL 18 container, so it proves the real
 * path: the context starts, Flyway applies the baseline migration, the database
 * answers, and the HTTP endpoints and CORS rules behave as configured.
 *
 * <p>The container is disposable and isolated. It never uses the local
 * development database or its volume, so running this test does not touch real
 * development data.
 *
 * <p>{@code @ServiceConnection} points the application datasource at this
 * container, which overrides the datasource in {@code application.yml}. The
 * required {@code DB_PASSWORD} property is supplied for test execution only
 * through the Failsafe plugin configuration.
 *
 * <p>The HTTP calls use the plain JDK {@link HttpClient}. It needs no extra
 * dependency and does not throw on a 4xx response, which keeps the CORS
 * rejection check (expecting 403) straightforward.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ReverieApplicationIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:18"));

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${local.server.port}")
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Sends a GET request to the running server. When {@code origin} is not null
     * it is sent as the Origin header, which makes the request a cross origin
     * request and exercises the CORS rules.
     */
    private HttpResponse<String> get(String path, String origin) throws Exception {
        HttpRequest.Builder request =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        if (origin != null) {
            request.header("Origin", origin);
        }
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void contextStartsAndDatabaseAnswers() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);

        assertThat(result).isEqualTo(1);
    }

    @Test
    void flywayBaselineMigrationIsRecorded() {
        List<String> appliedVersions = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success = true",
                String.class);

        assertThat(appliedVersions).contains("1");
    }

    @Test
    void healthEndpointReportsUp() throws Exception {
        HttpResponse<String> response = get("/actuator/health", null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
    }

    @Test
    void pingEndpointAnswersOk() throws Exception {
        HttpResponse<String> response = get("/api/v1/ping", null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"ok\"");
    }

    @Test
    void corsAllowsConfiguredOrigin() throws Exception {
        HttpResponse<String> response = get("/api/v1/ping", "http://localhost:4201");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin"))
                .contains("http://localhost:4201");
    }

    @Test
    void corsRejectsUnknownOrigin() throws Exception {
        HttpResponse<String> response = get("/api/v1/ping", "http://evil.example");

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
    }
}
