
# ADR-005: Email Verification and Delivery

- Status: Accepted
- Date: 2026-11-19
- Decision makers: Product owner and development team

## Context

Product decisions require mandatory email verification before a user can
participate in protected spaces. A production email provider is not selected yet,
and ARCHITECTURE.md section 14 defers that choice. Local development needs a way
to inspect outgoing mail without sending it to real recipients.

## Decision

- Email verification is mandatory before a user can access protected space,
  membership, or invitation actions. Unverified users may still access their own
  account and profile, login and logout, CSRF initialization, and the email
  verification and resend endpoints.
- Account status is separate from verification. The enabled flag means the account
  is active and a new account can authenticate before verifying. email_verified_at
  is the authorization gate for protected participation, not an authentication
  flag.
- Registration protects email existence. If the email is already registered,
  registration returns the same neutral response, creates no second account, and
  sends an "account already exists" notice email instead of a new verification
  token. The emailed verification link is the only channel that reveals account
  state, and only to the address that owns it.
- Email sending is hidden behind a small EmailSender port. Milestone 1 provides a
  single SMTP-based implementation. The production provider is chosen later and
  implements the same port without changing callers.
- Local development uses Mailpit as an SMTP sink (SMTP on port 1025, web UI on
  port 8025), configured through Spring Boot Mail. Mailpit runs as a container
  alongside PostgreSQL in local infrastructure.
- Verification tokens are generated with a cryptographically secure random
  generator, delivered only inside the emailed link, and stored only as a SHA-256
  hash. A fast cryptographic hash is appropriate here because the token is high
  entropy, unlike a password. Tokens are single-use and expire after 24 hours. A
  resend invalidates prior unconsumed tokens for the same user.
- Token consumption is atomic. A conditional UPDATE sets consumed_at only when the
  token is unconsumed and unexpired, and the same transaction marks the user
  verified. Concurrent verification of the same token succeeds exactly once.
- Tokens are never returned in API responses and never written to logs.

## Alternatives Considered

### Selecting a production provider now (for example Resend or Brevo)

Deferred: not needed for Milestone 1, and it would add a dependency and
credentials before they are required.

### Storing only a verified boolean on the user, with no token table

Disadvantages: no clean way to express single-use, expiry, or resend
invalidation. A dedicated token table is clearer and safer.

## Consequences

Positive:
- A complete, testable local verification flow.
- The provider is swappable through the port.
- Tokens are secure, single-use, and never exposed.

Negative:
- An email transport dependency is introduced in Milestone 1.
- Production delivery still needs a provider decision before public deployment.

## Reconsideration Conditions

Choose and document the production email provider before public deployment.
