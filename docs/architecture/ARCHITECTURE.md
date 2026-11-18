
# Reverie - System Architecture

Version: 1.0
Status: Proposed architecture baseline

## 1. Architecture Overview

Reverie uses a modular monolith architecture.

The system consists of:

- Angular frontend.
- Ionic and Capacitor for mobile applications.
- Java 21 and Spring Boot backend.
- PostgreSQL relational database.
- S3-compatible object storage for uploaded media.
- REST API for frontend/backend communication.

The frontend and backend are developed in one monorepository but deployed independently.

## 2. High-Level Architecture

```text
                 WEB BROWSER
                      |
                      |
              ANGULAR APPLICATION
                      |
                      |
                REST API / HTTPS
                      |
                      |
              SPRING BOOT BACKEND
                      |
          +-----------+-----------+
          |           |           |
       Identity     Memory      Space
        Module      Module     Module
          |           |           |
       Profile      Music       Story
        Module      Module     Module
          |           |           |
          +-----------+-----------+
                      |
               Repository Layer
                      |
                 PostgreSQL
                      |
              Object Storage
               (Photos/Media)
```

The mobile application uses the same Angular frontend through Ionic and Capacitor.

## 3. Technology Stack

### Frontend

- Angular
- TypeScript
- Ionic
- Capacitor
- Angular Router
- Angular HttpClient

Responsibilities:
- User interface.
- Navigation.
- Form validation for usability.
- API communication.
- Authentication state.
- Responsive web and mobile experience.

Frontend validation must never replace backend validation.

#### Current implementation (Milestone 0)

The following are actually on the backend classpath today:

- Java 21
- Spring Boot 4.1.2
- Spring Boot WebMVC starter (`spring-boot-starter-webmvc`)
- Spring Boot Actuator (health endpoint only)
- Spring JDBC (`spring-boot-starter-jdbc`), plain JDBC access with no ORM
- Flyway (`spring-boot-flyway`, `flyway-core`, `flyway-database-postgresql`)
- PostgreSQL JDBC driver (runtime)
- Maven (with the Maven wrapper pinned to 3.9.11)
- Testcontainers and Spring Boot Test (test scope only)

Milestone 0 deliberately uses plain Spring JDBC. There is no ORM, no
repository layer, and no domain tables yet.

#### Not yet implemented

The following are referenced by later milestones but are not present in the
Milestone 0 build:

- Spring Security. Authentication and authorization are deferred to Milestone 1.
- Bean Validation. Request validation is added when the first write endpoints
  arrive.
- Any object-relational mapping technology. The approach for domain persistence
  in Milestone 1 (for example plain JDBC, Spring Data JDBC, or Spring Data
  JPA/Hibernate) is undecided and must be chosen explicitly before Milestone 1
  implementation begins. This document does not commit to any of them.

Responsibilities (target for the full backend, reached incrementally):
- Authentication and authorization.
- Business logic.
- Privacy enforcement.
- Data validation.
- Database access.
- Media access management.
- REST API.

### Database

PostgreSQL stores:
- Users and profiles.
- Spaces and memberships.
- Invitations.
- Memories.
- Songs and their relationships to memories.
- Stories.
- Comments and reactions.
- Media metadata.

### Object Storage

Use S3-compatible object storage for photographs and uploaded media.

The database stores metadata and storage references, not the actual binary files.

Private media must not be publicly accessible through permanent public URLs.

## 4. Backend Module Structure

The backend is a modular monolith.

Initial logical modules:

### Identity
- Registration.
- Login.
- Logout.
- Authentication.
- User identity.

### Profile
- User profile.
- Profile settings.
- Profile visibility.

### Space
- Spaces.
- Space membership.
- Invitations.
- Roles and permissions.

### Memory
- Memory creation.
- Memory editing.
- Memory deletion.
- Timeline retrieval.
- Memory visibility.

### Music
- Song metadata.
- External music URLs.
- Song-to-memory associations.

### Story
- Story creation.
- Story editing.
- Story deletion.
- Story retrieval.

### Media
- Photograph uploads.
- Media metadata.
- Secure media access.

### Interaction
- Comments.
- Reactions.

### Common
- Shared error handling.
- Common validation.
- Security utilities.
- Shared infrastructure components.

Modules should communicate through clearly defined interfaces.

Avoid unnecessary dependencies between modules.

## 5. Suggested Backend Package Structure

```text
apps/api/
└── src/main/java/com/reverie/
    ├── ReverieApplication.java
    ├── identity/
    ├── profile/
    ├── space/
    ├── memory/
    ├── music/
    ├── story/
    ├── media/
    ├── interaction/
    └── common/
```

Each module may contain:

```text
module/
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
└── exception/
```

This is a target layout, not the current state. Milestone 0 contains only a
small `config` package and a single `web` controller. The `repository/` and
`entity/` folders, and the "Repository Layer" shown in the high-level diagram,
appear when domain persistence is introduced in Milestone 1. The shape of the
`repository/` and `entity/` layers depends on the persistence approach chosen
for Milestone 1, which is still undecided.

Do not create empty abstractions or unnecessary layers. Introduce structures when they provide a clear benefit.

## 6. Frontend Structure

Suggested Angular structure:

```text
apps/client/
└── src/app/
    ├── core/
    │   ├── auth/
    │   ├── guards/
    │   ├── interceptors/
    │   └── services/
    ├── shared/
    │   ├── components/
    │   └── models/
    ├── features/
    │   ├── home/
    │   ├── memories/
    │   ├── soundtracks/
    │   ├── spaces/
    │   └── profile/
    └── app.routes.ts
```

Use Angular standalone components and modern Angular conventions.

Avoid introducing state-management libraries until the application actually needs them.

## 7. Communication

The frontend communicates with the backend through REST endpoints over HTTPS.

Initial API conventions:

- JSON request and response bodies.
- Resource-oriented URLs.
- Appropriate HTTP status codes.
- Consistent error response structure.
- UUID identifiers.
- Pagination for collections.
- Server-side authorization.

API documentation should be maintained using OpenAPI.

## 8. Authentication

Use Spring Security.

The exact authentication mechanism must be evaluated and decided immediately before
Milestone 1. It is intentionally deferred so it does not block Milestone 0 scaffolding.

The initial design should consider:
- Secure password hashing.
- Short-lived access credentials.
- Secure refresh or session handling.
- CSRF protection where applicable.
- Secure cookie settings for browser clients.
- Secure token storage for mobile clients.
- Logout and credential revocation.

Do not store sensitive authentication tokens in browser localStorage by default.

## 9. Database Design Principles

- Use PostgreSQL as the source of truth.
- Use UUID identifiers for primary keys.
- Use Flyway for schema migrations.
- Define foreign-key constraints.
- Define appropriate unique constraints.
- Add indexes for common queries.
- Use transactions for multi-step business operations.
- Avoid unnecessary database relationships and abstractions.

The detailed entity model is documented separately in DATABASE_ERD.md.

## 10. Media Handling

Media uploads should follow this flow:

1. User requests permission to upload a file.
2. Backend verifies authentication and authorization.
3. Backend validates file type and size.
4. Backend stores the file in private object storage.
5. Backend saves media metadata in PostgreSQL.
6. Authorized users retrieve media through controlled access.

Never trust client-provided file names or MIME types without validation.

## 11. Deployment and Infrastructure

Local development:

- Docker Compose runs the PostgreSQL container only.
- The Spring Boot backend runs natively on the host (not containerized) and
  connects to PostgreSQL through the port Compose publishes on the host.
- The frontend development server runs natively on the host.

CI/CD:

- GitHub Actions.
- Automated backend tests.
- Frontend tests.
- Build verification.
- Dependency and security checks where practical.

Production hosting decisions are intentionally deferred.

## 12. Repository Structure

```text
reverie/
├── docs/
│   ├── product/
│   ├── architecture/
│   ├── decision/
│   └── development/
├── apps/
│   ├── client/
│   └── api/
├── infra/
│   └── docker-compose.yml
├── .github/
│   └── workflows/
└── README.md
```

## 13. Architectural Decisions

The following decisions are established:

- Modular monolith instead of microservices.
- Angular frontend shared across web and mobile.
- Java 21 and Spring Boot backend.
- PostgreSQL relational database.
- External music links for MVP.
- Object storage for media.
- REST API communication.
- Monorepository for frontend, backend, and documentation.

Changes to these decisions require an explicit discussion of alternatives and trade-offs.

## 14. Deferred Architectural Decisions

The following must be evaluated before their implementation:

- Exact authentication and session strategy.
- Object storage provider.
- Production hosting provider.
- Email delivery provider.
- Backup and disaster recovery strategy.
- Public profile and discovery infrastructure.

Do not introduce infrastructure for deferred features prematurely.
