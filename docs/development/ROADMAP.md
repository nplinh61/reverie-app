# Reverie Development Roadmap

## 1. Purpose

This roadmap defines the planned development sequence for the Reverie MVP.

Development should proceed in small, independently testable milestones. Each milestone must leave the application in a working state.

The roadmap may evolve as technical decisions are validated, but changes must be documented.

## 2. Development Principles

- Build one complete vertical slice at a time.
- Prefer working functionality over premature abstractions.
- Keep the backend as a modular monolith.
- Keep the frontend shared across web and mobile using Angular, Ionic, and Capacitor.
- Use Java 21 and Spring Boot for the backend.
- Write automated tests alongside implementation.
- Review security and authorization in every feature involving private data.
- Keep implementation decisions consistent with the product and architecture documents.
- Features outside the MVP scope are deferred to later milestones unless the scope is revised.

## 3. Milestones

### Milestone 0: Project Foundation

Status: Complete. Milestone 0 is the initial commit on `main`. Backend and
frontend checks passed in GitHub Actions CI.

Goal: Establish a reproducible development environment.

Planned work:
- Create the Angular/Ionic client application.
- Create the Spring Boot API application.
- Configure Java 21 and the required build tooling.
- Configure PostgreSQL using Docker Compose.
- Integrate Flyway and run a minimal baseline migration (no domain tables yet).
- Configure basic application health checks.
- Establish environment-specific configuration.
- Configure GitHub Actions for basic build and test execution.
- Add initial backend and frontend project documentation.

Acceptance criteria:

- Frontend and backend build successfully. Met.
- Automated checks run successfully in GitHub Actions CI. Met.

### Milestone 1: First Vertical Slice — Accounts and Spaces

Goal: Allow two users to create an account and collaborate in a shared space.

Prerequisite: the authentication and session strategy and the domain persistence
approach are decided (see ADR-002 and ADR-003). The full set of Milestone 1
decisions is recorded in ADR-002 through ADR-005 and the architecture documents.

Planned work:
- Implement user registration with email, password, display name, and a unique
  username. Protect email existence with a neutral response; report a username
  conflict as a clear field-level validation error.
- Implement mandatory email verification and resend, with Mailpit for local
  development.
- Implement login, logout, and current-user with cookie sessions and CSRF.
- Implement basic user profiles (display name and optional bio; username read-only
  in Milestone 1).
- Implement spaces, membership, and the OWNER and MEMBER roles.
- Implement invitation creation (EMAIL single-use and SHAREABLE usage-limited),
  listing, preview, revocation, and concurrency-safe acceptance.
- Enforce backend authorization rules, including the 401, 403, and 404 policy and
  rate limiting.
- Provide minimal responsive web screens for the flows above, including mobile
  Safari, without the full visual design and without native Capacitor builds.
- Add integration, security, and concurrency tests.

Acceptance criteria:
- User A can register, verify their email, and log in.
- An unverified user cannot participate in protected spaces.
- User A can create a space and is its OWNER.
- User A can invite User B by email or by a shareable link.
- User B can accept the invitation.
- Both users can access the space.
- An unauthorized user cannot access the space.
- A user cannot access another user's private resources.

### Milestone 2: Memories and Timeline

Goal: Allow users to create and revisit memories.

Planned work:
- Implement memory creation.
- Implement memory editing and deletion by the creator.
- Implement PRIVATE and SPACE_ONLY visibility.
- Implement memory listing and chronological timeline.
- Implement pagination and filtering where appropriate.
- Enforce memory ownership and visibility rules.
- Add backend authorization and integration tests.

Acceptance criteria:
- A user can create a memory in an authorized space.
- A user can edit and delete only their own memories.
- Private memories are visible only to their creator.
- Space-only memories are visible to authorized space members.
- Memories appear in chronological order.
- Unauthorized access is rejected by the backend.

### Milestone 3: Photo Uploads and Media Access

Goal: Allow users to attach photographs to memories and manage profile avatars.

Planned work:
- Configure S3-compatible object storage.
- Implement media metadata and storage integration.
- Implement MEMORY_PHOTO and PROFILE_AVATAR purposes.
- Implement upload validation and file size limits.
- Implement secure media access.
- Enforce ownership and visibility rules.
- Add media cleanup handling and tests.

Acceptance criteria:
- Users can upload valid photographs to their memories.
- Users can set their own profile avatar.
- Invalid or oversized files are rejected.
- Private media cannot be accessed by unauthorized users.
- Media purpose and memory association constraints are enforced.

### Milestone 4: Music and Soundtracks

Goal: Allow users to associate songs with memories.

Planned work:
- Implement song metadata.
- Support external music links.
- Associate multiple songs with a memory.
- Validate supported platforms and URLs.
- Display songs in memory details.

Acceptance criteria:
- Users can add external song links to their memories.
- A memory can contain multiple songs.
- Songs can be opened through their external platforms.
- Reverie does not host or stream copyrighted music.

### Milestone 5: Stories

Goal: Allow users to preserve longer written experiences.

Planned work:
- Implement story creation and editing.
- Support stories attached to memories.
- Support stories without an associated memory.
- Enforce story ownership.
- Enforce inherited memory visibility and space access.
- Add integration and authorization tests.

Acceptance criteria:
- Users can create stories in authorized spaces.
- Only the creator can edit or delete their stories.
- Stories attached to private memories remain private.
- Stories without a memory association are accessible only to authorized space members.

### Milestone 6: Comments and Reactions

Goal: Allow members to interact with accessible memories.

Planned work:
- Implement comments.
- Implement reactions.
- Allow users to edit and delete their own comments.
- Allow users to update and remove their own reactions.
- Enforce memory access permissions.
- Add integration and authorization tests.

Acceptance criteria:
- Authorized members can comment on accessible memories.
- Authorized members can react to accessible memories.
- Users cannot modify other users' comments or reactions.
- Users cannot interact with memories they cannot access.

### Milestone 7: Frontend Integration and Mobile Support

Goal: Deliver a coherent user experience across web and mobile.

Planned work:
- Implement authentication screens.
- Implement home and timeline views.
- Implement memory creation and detail screens.
- Implement space management and invitations.
- Implement profile and settings screens.
- Integrate frontend with backend APIs.
- Add responsive layouts.
- Configure Capacitor for mobile builds.
- Test core flows on supported mobile platforms.

Acceptance criteria:
- Users can complete the core MVP workflows through the frontend.
- The web application works on desktop and mobile browsers.
- The mobile application can be built using Capacitor.
- Loading, empty, and error states are handled.
- Frontend restrictions do not replace backend authorization.

### Milestone 8: Testing, Deployment, and MVP Release

Goal: Prepare Reverie for real users.

Planned work:
- Complete backend integration and security testing.
- Add frontend tests for critical workflows.
- Review privacy and authorization behavior.
- Configure production environment variables and secrets.
- Set up production database and object storage.
- Configure deployment pipeline.
- Add basic monitoring and error reporting.
- Prepare backup and recovery procedures.
- Conduct a small user acceptance test with 5–10 initial testers.
- Collect feedback and prioritize improvements.

Acceptance criteria:
- The core MVP workflows work reliably.
- Critical authorization tests pass.
- Production secrets are not committed to Git.
- Database backup and recovery procedures are documented.
- Initial testers can use the deployed application.
- Known limitations and issues are documented.

## 4. Deferred Features

The following features are intentionally outside the initial MVP:

- AI-generated memories, summaries, or recommendations.
- Public profiles and public discovery.
- Public memory sharing.
- Native real-time messaging.
- Spotify API integration or music streaming.
- Advanced social feeds.
- Complex moderation and administrative permissions.
- Account deletion and shared-content ownership transfer.
- Microservices architecture.
- Advanced analytics and recommendation systems.

These features may be reconsidered after the MVP has been validated.

## 5. Milestone Completion Rules

A milestone is complete only when:

- Its acceptance criteria are met.
- Relevant automated tests pass.
- Security and authorization requirements are tested where applicable.
- Documentation is updated.
- The implementation has been reviewed.
- No unrelated or unapproved features have been introduced.

## 6. Change Management

- Do not silently change product requirements.
- Record significant architectural decisions as ADRs.
- Update this roadmap when scope or milestone order changes.


## 7. Current Priority

Milestone 0 is complete and present on `main` as the initial commit.

The next milestone is Accounts and Spaces (M1). Its architecture and security
decisions are finalized and recorded in ADR-002 through ADR-005 and the
architecture documents. The first M1 feature, user registration with email
verification, is implemented.

## 8. Non-blocking Follow-ups

These are tracked improvements that do not block the current milestone:

- The Angular initial bundle exceeds the default 500 kB budget by roughly 72 kB
  after adding `@angular/forms` for the registration screens. The production
  build still succeeds; this is a warning only. The bundle budget is
  intentionally left unchanged. Revisit when the frontend grows, for example with
  route-level lazy loading or a deliberately reasoned budget adjustment, rather
  than raising the budget just to silence the warning.
