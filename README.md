
# Reverie

**A place for the moments, music, and stories that stay with us.**

Reverie is a music and memory platform designed to help individuals, friends, and couples preserve meaningful moments.

Users can create memories, attach music through external links, write stories, and share personal spaces with people who matter to them.

## Product Vision

Reverie brings three things together:

- **Memories:** Preserve meaningful moments, dates, and photographs.
- **Music:** Connect songs to memories through external music links.
- **Stories:** Give memories context through personal writing.

## Technology Stack

| Layer | Technology |
|---|---|
| Frontend | Angular 22 + TypeScript (standalone components) |
| Mobile | Ionic 9 + Capacitor 8 |
| Backend | Java 21 + Spring Boot 4 |
| Database access | Spring JDBC (no ORM in Milestone 0) |
| Database | PostgreSQL 18 |
| Database migrations | Flyway |
| API | REST |
| Infrastructure | Docker Compose (PostgreSQL; the backend runs natively) |
| Tests | Vitest (frontend), JUnit and Testcontainers (backend) |
| CI | GitHub Actions |
| Version control | Git + GitHub |

The Milestone 0 backend uses plain Spring JDBC. JPA, Hibernate, and Spring
Security are not used yet; the persistence and authentication approaches are
decided before Milestone 1.

## Repository Structure

```text
reverie/
├── docs/
├── apps/
│   ├── client/
│   └── api/
├── infra/
├── .github/
└── README.md
```

## Local Development

PostgreSQL runs in a Docker Compose container; the backend and the frontend dev
server run natively on the host. Detailed, platform-specific setup lives in the
application READMEs rather than being duplicated here:

- Backend: [`apps/api/README.md`](apps/api/README.md)
- Frontend: [`apps/client/README.md`](apps/client/README.md)

Copy `.env.example` to `.env` at the repository root before starting the
database or the backend.

## Development Philosophy

Reverie is developed incrementally.

The focus is on a reliable, secure, maintainable MVP that can be tested with real users before expanding its functionality.


## Project Status

Milestone 0 (Project Foundation) is complete and merged into `main`.

The foundation includes an Angular frontend, a Spring Boot backend,
PostgreSQL via Docker Compose, Flyway migrations, health checks,
automated tests, and GitHub Actions CI.

Both backend and frontend CI jobs have passed.

No domain features have been implemented yet. Milestone 1
(Accounts and Spaces) is next.

See [Development Roadmap](docs/development/ROADMAP.md)
for planned milestones.