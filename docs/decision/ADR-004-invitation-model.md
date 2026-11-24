
# ADR-004: Space Invitation Model and Acceptance

- Status: Accepted
- Date: 2026-11-19
- Decision makers: Product owner and development team

## Context

DATABASE_ERD.md Open Question 5 asked whether invitations should be
email-specific or shareable by link. Milestone 1 needs both. The original
single-use invitation schema (a single invited email and a single accepting user)
cannot represent a reusable link with a usage limit. Acceptance must be safe under
concurrency, must not expose database errors, and must not disclose private
information.

## Decision

Support two invitation types and a concurrency-safe acceptance flow.

Types:
- EMAIL: single-use, bound to a specific invited email. max_uses is 1.
- SHAREABLE: a reusable link with an owner-configured usage limit. Minimum 1,
  default 10, maximum 50.

Both types have a default expiry of seven days, are revocable, and have immutable
terms after creation. To change terms, the owner revokes and recreates.

Status is derived from stored facts rather than stored as an enum. Precedence:
REVOKED (revoked_at set), then EXPIRED (past expires_at), then EXHAUSTED
(uses_consumed is at least max_uses), otherwise ACTIVE.

Acceptance is a single transaction at READ COMMITTED isolation, membership first:

1. Validate the invitation from its stored facts and check eligibility. The user
   must have a verified email. For EMAIL invitations the invited email must match
   the authenticated user's email. If invalid, return the uniform unavailable
   response.
2. Insert the membership with INSERT ... ON CONFLICT (space_id, user_id) DO
   NOTHING RETURNING. If no row is created, the user is already a member: return
   idempotent success and consume no use.
3. If a membership row was created, claim one use with a conditional UPDATE
   guarded by revoked_at IS NULL AND expires_at > now() AND uses_consumed <
   max_uses. If it affects zero rows, throw a domain exception so the whole
   transaction rolls back and the membership does not persist.
4. Record the acceptance with INSERT ... ON CONFLICT (invitation_id, user_id) DO
   NOTHING.

No unique-violation exception is ever caught to drive a success response. In
PostgreSQL a constraint error aborts the transaction, so ON CONFLICT DO NOTHING is
used to avoid raising it. All control flow uses row counts and one intentional
rollback. Expected domain outcomes map to 4xx responses; unexpected database
errors map to a generic 500 and are never swallowed into success.

Unavailable invitation tokens return a uniform HTTP 404 that does not reveal
whether the token was unknown, expired, revoked, or exhausted.

Public preview, keyed by the raw token, returns the space name and inviter display
name only when the invitation is valid. Every non-valid state returns the same
unavailable response with no space or inviter data.

Ownership and membership: a space always retains an OWNER. In Milestone 1 an owner
cannot leave or remove themselves, and ownership transfer is deferred. Members who
are already in a space do not consume an invitation use.

## Alternatives Considered

### Nesting acceptance history inside the Invitation aggregate with optimistic locking

Disadvantages: an aggregate save rewrites its children, needs @Version and retry
loops, and is more complex than a conditional UPDATE. Rejected.

### Catching unique-constraint violations to return idempotent success

Disadvantages: in PostgreSQL the transaction is aborted after a constraint error,
so continuing to issue statements is unsafe. Rejected in favor of ON CONFLICT DO
NOTHING and row-count checks.

## Consequences

Positive:
- Exactly one membership per user and space.
- At most the configured number of uses, enforced under concurrency.
- Predictable idempotent behavior for duplicate requests.
- No database errors exposed through the API.
- Correct behavior when two invitations for the same space are accepted
  concurrently by the same user.

Negative:
- Invariants live partly in SQL and database constraints, so they must be covered
  by concurrency tests.

## Reconsideration Conditions

Reconsider if invitation features grow, for example per-invitee tracking on
shareable links or editable terms after creation.
