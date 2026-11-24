
# ADR-003: Authentication and Session Strategy

- Status: Accepted
- Date: 2026-11-19
- Decision makers: Product owner and development team

## Context

Milestone 1 adds accounts and protected resources, so the authentication decision
deferred in ARCHITECTURE.md section 8 must now be made. Milestone 1 targets
responsive web including mobile Safari. Native Capacitor builds are deferred to
Milestone 7. The product runs as a single instance in Milestone 1.

## Decision

Use Spring Security with server-managed, cookie-based sessions.

- Sessions are stored in server memory for the single-instance Milestone 1
  deployment. A persistent store (for example Spring Session backed by PostgreSQL)
  is reconsidered before any multi-instance deployment.
- The session identifier is carried in a session-scoped JSESSIONID cookie with
  HttpOnly, SameSite=Lax, and Secure in production. Secure is disabled on
  plain-HTTP localhost development. The inactivity timeout is two hours. There is
  no remember-me in Milestone 1.
- Login, logout, and current-user are handled by an explicit AuthController that
  returns JSON, not by Spring form-login redirects. Login authenticates through
  the AuthenticationManager and stores the SecurityContext in the session. Logout
  invalidates the server session and clears cookies.
- Login is by email and password. Email is normalized by trimming and lowercasing,
  applied consistently for registration, login, and email-specific invitation
  matching. Provider-specific normalization is not implemented.
- Account status is distinct from verification. The enabled flag means the account
  is active, and a new account can authenticate before verifying its email.
  email_verified_at is a separate authorization gate for protected spaces,
  membership, and invitations (see ADR-005). Registration protects email existence
  only; a username conflict returns a clear field-level validation error.
- CSRF protection is enabled using a cookie token repository that JavaScript can
  read (CookieCsrfTokenRepository.withHttpOnlyFalse) together with a
  CsrfTokenRequestAttributeHandler. The cookie and header use project-specific
  names, REVERIE-XSRF-TOKEN and X-REVERIE-XSRF-TOKEN, rather than the framework
  default XSRF-TOKEN, so the cookie does not collide with another application that
  shares the same host during local development (cookies are not isolated by
  port). The web client reads the REVERIE-XSRF-TOKEN cookie and echoes it as the
  X-REVERIE-XSRF-TOKEN header. A dedicated public endpoint,
  GET /api/v1/auth/csrf, initializes the cookie and requires no authentication.
  The lifecycle is explicit: before login the client calls GET /auth/csrf so the
  REVERIE-XSRF-TOKEN cookie exists before the first mutating request; after login
  the response writes the rotated token; after logout the cookie is cleared and
  the client calls
  GET /auth/csrf again before the next login. This behavior is verified by
  integration tests covering the full flow (see SECURITY.md). The design does not
  assume that token rotation silently writes the cookie.
- Same-origin deployment is assumed. In development the Angular dev server proxies
  /api to the backend, so all browser traffic is same-origin and uses relative
  URLs. In production a reverse proxy routes /api to the backend under one
  registrable domain. Cross-origin credentialed requests are therefore not on the
  normal path. The existing CORS allowlist is kept as defense in depth and served
  through Spring Security's CorsConfigurationSource.
- Passwords are hashed with BCrypt at cost factor 12 through a
  DelegatingPasswordEncoder, so the stored value carries an algorithm identifier
  and can be migrated later. The cost factor is benchmarked before production.
  Passwords must be at least 10 characters and at most 72 UTF-8 bytes. Input is
  preserved exactly with no Unicode normalization, and over-limit input is
  rejected rather than truncated.

## Alternatives Considered

### Stateless JWT access tokens

Advantages: no server session state, scales horizontally.

Disadvantages: revocation and logout are harder, safe browser storage is a
concern (localStorage is discouraged), and the added complexity is not justified
for a single-instance MVP.

### Cookie sessions with a persistent store from day one

Advantages: survives restarts, ready for multiple instances.

Disadvantages: extra infrastructure and a session schema before they are needed.
Deferred until deployment planning.

## Consequences

Positive:
- Simple, well-understood browser authentication with first-party cookies and
  little client code.
- Logout truly invalidates the session, with no client token storage concerns.
- Same-origin routing removes cross-origin cookie and CSRF-header pitfalls.

Negative:
- In-memory sessions are lost on restart and do not scale across instances. This
  is accepted for Milestone 1 and revisited before deployment.
- Native Capacitor clients will need session handling re-verified in Milestone 7.

## Reconsideration Conditions

Reconsider when deploying multiple instances, when native mobile clients require a
different credential model, or when session persistence across restarts becomes a
requirement.
