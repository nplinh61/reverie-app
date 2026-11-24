# Reverie Security Baseline

## 1. Purpose

This document defines the minimum security and privacy requirements for the Reverie MVP.

Security must be enforced by the backend. Frontend restrictions are useful for user experience but must never be treated as authorization.

## 2. Authentication

- Users must authenticate before accessing private account data or protected resources.
- Passwords must never be stored in plain text.
- Passwords must be hashed using a modern, secure password-hashing algorithm.
- Authentication credentials and tokens must never be logged.
- Authentication failures must return generic messages that do not reveal whether an account exists.
- Registration, login, and other sensitive endpoints must have appropriate rate limiting.
- The authentication mechanism and session strategy must be documented before implementation.
- Logout must invalidate the relevant session or refresh token.

Milestone 1 decisions (see ADR-003 and ADR-005):
- Passwords are hashed with BCrypt at cost 12 through a DelegatingPasswordEncoder,
  benchmarked before production.
- Passwords must be at least 10 characters and at most 72 UTF-8 bytes. The byte
  limit is enforced with a custom constraint, not a character-count constraint,
  because BCrypt operates on 72 bytes and silently truncates beyond that. The
  password is preserved exactly, with no Unicode normalization, and over-limit
  input is rejected rather than truncated.
- Authentication uses server-managed cookie sessions. The session cookie is
  session-scoped, HttpOnly, SameSite=Lax, and Secure in production. The inactivity
  timeout is two hours. There is no remember-me in Milestone 1.
- Login is by email and password. Email is normalized by trimming and lowercasing,
  applied consistently for registration, login, and email-specific invitations.
  Provider-specific normalization is not implemented.
- Registration protects email existence only. The response is neutral and does not
  reveal whether the email is already registered. A username conflict returns a
  clear 400 field-level validation error, because a username is a public handle
  rather than a secret. PostgreSQL unique constraints on email and username remain
  authoritative, including under concurrent registration.
- Duplicate-email registration always returns the same neutral response. No second
  account is created, and an "account already exists" notice email is sent instead
  of a new verification token.
- Account status: enabled means the account is active, and new accounts can
  authenticate before verification. email_verified_at is a separate authorization
  gate, not an authentication flag.
- Email verification is mandatory before a user can participate in protected
  spaces, membership, or invitations. An unverified user may access their own
  account and profile, login and logout, CSRF initialization, and the verification
  and resend endpoints. Verification tokens are single-use, expire after 24 hours,
  are stored only as SHA-256 hashes, are never returned in responses or logged,
  and are consumed atomically.

## 3. Authorization

- Every protected backend operation must verify that the authenticated user has permission to perform it.
- Authorization must be enforced centrally in the backend service layer.
- Knowing a resource's UUID must never grant access to it.
- Users may access a private space only if they are authorized members.
- Space membership grants access according to the resource's visibility and the user's permissions.
- Space ownership does not automatically grant ownership of memories created by other members.
- Only the creator of a memory may edit or delete that memory.
- Other authorized space members may view accessible shared memories and may comment on or react to them.
- Users may edit or delete only their own comments and reactions in the MVP.
- A user must not gain access to another user's private profile or private content by changing an API request.

Response code policy for authorization (Milestone 1):
- 401 when the request is not authenticated.
- 404 for visibility boundaries, where the user is not permitted to know the
  resource exists, such as a non-member of a private space. The response must be
  uniform so it does not disclose existence.
- 403 for action or role denial on a resource the user may already see, such as a
  non-OWNER member attempting invitation management, or an unverified user
  attempting protected space actions.
- A space always retains an OWNER. In Milestone 1 an owner cannot leave or remove
  themselves, and ownership transfer is deferred. Member removal must reject
  removing the sole OWNER.

## 4. Privacy and Visibility

- MVP memory visibility is limited to `PRIVATE` and `SPACE_ONLY`.
- `PRIVATE` memories are accessible only to their creator.
- `SPACE_ONLY` memories are accessible only to authorized members of the associated space.
- Public profiles, public memories, and public discovery are outside the MVP scope.
- A private space must never expose its content publicly.
- Stories attached to a memory inherit that memory's visibility.
- A story without an associated memory belongs to its space and is accessible only to authorized members of that space.
- Comments and reactions must not be accessible to users who cannot access their associated memory.
- API responses must not expose private information through nested objects, counts, previews, or metadata.

## 5. Invitations

- Invitation tokens must be generated using a cryptographically secure random generator.
- Only a hash of an invitation token should be stored in the database.
- Invitation tokens must have an expiration time.
- Invitations must be revocable.
- An invitation may be accepted only while it is valid, unexpired, and not previously used.
- Accepting an invitation must verify the authenticated user's identity.
- Invitation endpoints must be protected against brute-force attempts.
- Responses must not reveal unnecessary information about private spaces.

Milestone 1 decisions (see ADR-004):
- Two types: EMAIL (single-use, bound to an invited email) and SHAREABLE
  (reusable with a usage limit of minimum 1, default 10, maximum 50).
- Status is derived from stored facts (revoked_at, expires_at, uses_consumed,
  max_uses), not stored as an enum, so authorization never relies on a stale
  status value.
- Unavailable tokens return a uniform HTTP 404 that does not reveal whether the
  token was unknown, expired, revoked, or exhausted.
- Public preview returns the space name and inviter display name only when the
  invitation is valid; otherwise it returns the same unavailable response.
- Acceptance is concurrency-safe. It inserts membership with ON CONFLICT DO
  NOTHING and claims a use with a conditional UPDATE in a single transaction. It
  never catches a unique-constraint violation to drive success, because that would
  run inside an aborted PostgreSQL transaction. Exactly one membership is created
  per user and space, and no extra uses are consumed.

## 6. Media and File Uploads

- Uploaded files must be validated by the backend.
- Validation must consider file type, actual content, file size, and allowed formats.
- File names supplied by users must never be trusted as storage paths.
- Uploaded media must use generated storage identifiers.
- Memory photographs must be associated with an authorized memory.
- Profile avatars must use the `PROFILE_AVATAR` media purpose and must not require a memory association.
- Memory photographs must use the `MEMORY_PHOTO` media purpose and must reference a memory.
- The database must enforce the relationship between media purpose and memory association.
- Private media must not be publicly accessible through permanent storage URLs.
- Media access must verify the requesting user's permissions.
- Upload and download endpoints must enforce file size limits.
- The MVP must not allow arbitrary executable files to be uploaded.

## 7. Input Validation and API Security

- All incoming request data must be validated on the backend.
- The backend must enforce required fields, length limits, valid formats, and allowed enum values.
- Database queries must use parameterized queries or safe ORM mechanisms.
- User-provided content must be safely rendered by the frontend.
- The API must use HTTPS in deployed environments.
- Cross-origin access must be explicitly configured rather than allowing every origin.
- If cookie-based authentication is selected, CSRF protection and secure cookie settings must be implemented.
- Sensitive endpoints must have appropriate rate limits.
- Error responses must not expose stack traces, SQL details, secrets, or internal implementation information.

CSRF initialization and refresh (Milestone 1, cookie sessions, see ADR-003):
- CSRF uses a cookie token that JavaScript can read and that the client echoes as
  the X-REVERIE-XSRF-TOKEN header. The cookie and header use project-specific
  names (REVERIE-XSRF-TOKEN and X-REVERIE-XSRF-TOKEN) instead of the framework
  default XSRF-TOKEN, so the cookie does not collide with another application on
  the same host during local development.
- A dedicated public endpoint, GET /api/v1/auth/csrf, initializes the cookie. It
  requires no authentication and is permitted for anonymous requests.
- Before login: the client calls GET /api/v1/auth/csrf so the REVERIE-XSRF-TOKEN
  cookie exists before the first mutating request, including the login POST.
- After login: the session is rotated and a new CSRF token is issued. The login
  response writes the new REVERIE-XSRF-TOKEN cookie value, and the client uses it
  for subsequent mutating requests.
- After logout: the server invalidates the session and clears the REVERIE-XSRF-TOKEN
  cookie. The client calls GET /api/v1/auth/csrf again before the next login.
- This behavior must be verified by integration tests covering the full flow
  before login, after login, and after logout. It must not be assumed that token
  rotation silently writes the cookie.

Rate limiting (Milestone 1, see ADR-003):
- Implemented with Bucket4j in memory for the single instance. Responses are 429
  with a Retry-After header and a generic body.
- Login uses two independent buckets, one per client IP and one per normalized
  email, and both must permit the request. This blunts both spraying from one host
  and targeted guessing across many hosts.
- Registration is limited per IP. Verification resend is limited per account and
  per IP. Invitation preview and accept are limited per IP with a global ceiling,
  and per user on accept.
- Account-keyed limits use short windows and backoff rather than hard lockouts, to
  avoid a lockout denial of service, and do not reveal lockout state.

Trusted-proxy and client IP handling:
- X-Forwarded-For is client-spoofable. It is honored only when the immediate peer
  is a configured trusted proxy, using the right-most trusted hop. Forwarded-header
  trust is off by default in development.

## 8. Account and Data Lifecycle

- Account deletion behavior must be explicitly designed before implementation.
- Deleting an account must not silently delete or expose shared content belonging to other users.
- The MVP must not implement destructive account deletion without a documented policy.
- Data retention and cleanup behavior for expired invitations, consumed or expired
  verification tokens, and orphaned media must be documented. The cleanup
  implementation may be deferred, but the policy must be written down.
- Personal data must not be included in application logs unless strictly necessary.

## 9. Secrets and Configuration

- Passwords, API keys, database credentials, signing keys, and storage credentials must never be committed to Git.
- Local secrets must be stored outside tracked source files.
- Environment-specific configuration must be separated from application code.
- Example environment files may contain placeholders but never real credentials.
- Production secrets must be provided through an appropriate deployment secret-management mechanism.

## 10. Logging and Monitoring

- Application logs must not contain passwords, authentication tokens, invitation tokens, or private story and memory content.
- Security-relevant events such as repeated authentication failures and rejected access attempts should be logged.
- Logs should identify relevant resource IDs and event types without unnecessarily exposing personal data.
- Unexpected authorization failures and server errors should be observable.

## 11. Security Testing

The backend must include tests for at least the following cases:

- An unauthenticated user cannot access protected endpoints.
- A non-member cannot access a private space.
- A space member cannot access another member's private memory.
- A space member cannot edit or delete another member's memory.
- A space owner cannot edit or delete another member's memory merely because they own the space.
- A user cannot edit or delete another user's comment or reaction.
- An expired, revoked, or already-used invitation cannot be accepted.
- A user cannot download media they are not authorized to access.
- A story attached to a private memory cannot be exposed to other space members.
- Invalid and oversized uploads are rejected.

Additional Milestone 1 cases:
- An unverified user is blocked from protected space, membership, and invitation
  endpoints, but can access their own account, verification, and resend.
- Registration does not reveal whether an email already exists, and duplicate-email
  registration creates no second account.
- A username conflict returns a clear 400 field-level validation error, and
  concurrent registration with the same email or username is rejected by the
  database.
- A password shorter than 10 characters or longer than 72 UTF-8 bytes is rejected,
  and the password is not altered before hashing.
- Email is normalized by trimming and lowercasing for registration, login, and
  email-specific invitations.
- CSRF is required on mutating requests. An integration test covers the full flow:
  the GET /api/v1/auth/csrf cookie is initialized before login, refreshed on
  login, and cleared on logout.
- An unavailable invitation token returns a uniform 404 for both preview and
  accept, with no state disclosure.
- An EMAIL invitation cannot be accepted by a different email than invited.
- Concurrent invitation acceptance is correct: the last available use contested by
  two users admits exactly the limit, duplicate requests create one membership and
  consume one use, and two invitations for the same space create one membership.
- Rate limits return 429, and X-Forwarded-For is trusted only from a configured
  proxy.

## 12. Security Development Principles

- Security requirements must be considered during design, not added only after implementation.
- Backend authorization must be tested independently of frontend behavior.
- New endpoints must define their authentication and authorization requirements.
- Changes to privacy rules must be reflected in the product specification, API design, database design, and tests.
- The MVP should prefer simple, explicit security rules over premature abstractions.
