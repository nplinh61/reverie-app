# Reverie API

Spring Boot backend for Reverie. This is the Milestone 0 technical foundation.

## Technology

- Java 21
- Spring Boot 4.1.2 (WebMVC starter, Actuator, Spring JDBC)
- Flyway for database migrations (`spring-boot-flyway`, `flyway-core`,
  `flyway-database-postgresql`)
- PostgreSQL 18 (runs through Docker Compose; see `infra/docker-compose.yml`)
- Maven, with the Maven wrapper pinned to 3.9.11
- Testcontainers and Spring Boot Test for the integration test (test scope)

Milestone 0 uses plain Spring JDBC only. There is no JPA, no Hibernate, no Spring
Security, and no Bean Validation. The domain persistence approach for Milestone 1
is undecided and will be chosen before Milestone 1 implementation begins.

## Current state (Milestone 0, complete through Phase 8)

The backend provides:

- `GET /api/v1/ping` returns `{"status":"ok"}`. It does not touch the database.
- `GET /actuator/health` returns the health status only, with no details.
- A CORS policy that allows exactly `http://localhost:4200` for `/api/v1/**`.
- An environment-driven PostgreSQL datasource.
- A fail-fast check: `DB_PASSWORD` is required at startup and has no default. When
  it is missing or blank, the application stops with a clear message.
- The Flyway baseline migration `V1__baseline.sql`. It is intentionally empty: it
  runs `SELECT 1` and creates no database objects. Its only purpose is to
  establish the Flyway version history. The real schema starts at `V2` in
  Milestone 1.

There are no domain tables and no business endpoints yet, by design.

## Prerequisites

- JDK 21.
- Docker Desktop, for the local PostgreSQL container and for the Testcontainers
  integration test.
- A `.env` file at the repository root. Copy it from `.env.example` and keep it
  out of version control:

  ```powershell
  # Windows PowerShell, from the repository root
  Copy-Item .env.example .env
  ```

## Run locally

These commands use Windows PowerShell and are run from the repository root unless
noted. The backend runs natively on the host; only PostgreSQL runs in Docker.

1. Start PostgreSQL:

   ```powershell
   docker compose --env-file .env -f infra/docker-compose.yml up -d
   ```

2. Provide `DB_PASSWORD` to the backend process and start it. The value must
   match `DB_PASSWORD` in your `.env`:

   ```powershell
   # from apps/api
   $env:DB_PASSWORD = "<your-local-db-password>"
   .\mvnw.cmd spring-boot:run
   ```

   The application listens on its default port 8080. Check connectivity:

   ```powershell
   curl http://localhost:8080/api/v1/ping
   ```

## Build and test

Run from this directory (`apps/api`). The Maven wrapper is pinned to 3.9.11.

```powershell
.\mvnw.cmd clean verify     # Windows PowerShell
```

```bash
./mvnw clean verify         # Linux / macOS, and what CI runs
```

`verify` runs both test phases:

- Surefire runs the unit and web-layer tests (`*Test`). `PingControllerTest` uses
  a standalone MockMvc setup and starts no Spring context or database.
  `DbPasswordValidatorTest` is a plain unit test.
- Failsafe runs the integration test (`ReverieApplicationIT`). It starts the full
  application against a throwaway PostgreSQL 18 Testcontainers database, so it
  requires Docker to be running. It verifies the database connection, the Flyway
  baseline, the health and ping endpoints, and both the allowed and rejected CORS
  cases.

The build supplies a dummy `DB_PASSWORD` to the test plugins so the fail-fast
startup check passes during tests. This value is for test execution only. It is
never an application default and is not a real secret.

## Continuous integration


Milestone 0 is complete and merged into `main`. Backend and frontend
checks have passed in GitHub Actions CI.

See the [Development Roadmap](../../docs/development/ROADMAP.md)
for milestone status and planned work.
