
# ADR-001: Use a Modular Monolith

- Status: Accepted
- Date: 2026-11-10
- Decision makers: Product owner and development team

## Context

Reverie is an early-stage application combining memories, music, stories, user profiles, and shared spaces.

The project is initially developed and maintained by a single developer, so keeping the architecture manageable is important.

The application needs a maintainable architecture without unnecessary operational complexity.

## Decision

Use a modular monolith for the backend.

The backend will be one Spring Boot application with clearly separated business modules.

Initial modules:

- Identity
- Profile
- Space
- Memory
- Music
- Story
- Media
- Interaction
- Common

## Alternatives Considered

### Microservices

Advantages:
- Independent deployment.
- Independent scaling.
- Clear service boundaries.

Disadvantages:
- More infrastructure.
- Network communication complexity.
- Distributed transactions and consistency challenges.
- Higher development and operational overhead.

### Traditional Monolith Without Module Boundaries

Advantages:
- Simple initial setup.
- Minimal infrastructure.

Disadvantages:
- Business responsibilities can become tightly coupled.
- Harder to maintain as the application grows.

## Consequences

Positive:
- Simpler deployment.
- Easier local development.
- Clear business boundaries.
- Lower operational overhead.
- Easier automated testing.

Negative:
- Modules share one deployment lifecycle.
- Boundaries must be enforced through code conventions and reviews.

## Reconsideration Conditions

Reconsider this decision only when measurable scaling, organizational, or deployment requirements justify independent services.
