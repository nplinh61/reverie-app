
# Reverie - REST API Design

Version: 1.0
Status: Proposed API baseline

## 1. API Conventions

Base path:

/api/v1

General conventions:

- REST-oriented resource URLs.
- JSON request and response bodies.
- UUID resource identifiers.
- HTTPS in production.
- Consistent error responses.
- Backend validation.
- Backend authorization.
- Pagination for collection endpoints.
- OpenAPI documentation.

## 2. Authentication

| Method | Endpoint | Description |
|---|---|---|
| POST | /auth/register | Register an account |
| POST | /auth/login | Authenticate |
| POST | /auth/logout | Log out |
| GET | /auth/me | Get the current user |
| GET | /auth/csrf | Initialize the CSRF cookie (public, unauthenticated) |
| POST | /auth/verify-email | Verify an email with a token |
| POST | /auth/resend-verification | Resend a verification email |

Authentication uses Spring Security server-managed cookie sessions (see ADR-003).

Registration:
- Registration requires email, password, display name, and username.
- Email existence is protected. Registration returns a neutral response that does
  not reveal whether the email is already registered. Login failures are likewise
  generic and do not reveal whether an account exists.
- Username existence is not protected because a username is a public handle, not a
  secret, and a taken username cannot silently create an account. A username
  conflict returns a clear 400 field-level validation error (for example
  "username already taken"). An optional GET username-availability check may be
  added later.
- PostgreSQL unique constraints on email and username remain authoritative,
  including under concurrent registration. A race that passes the application
  pre-check is still rejected by the database, and the backend maps that rejection
  to the same neutral email response or the same username field error.
- Duplicate-email handling: registration always returns the same neutral
  success-style response. If the email is already registered, no second account is
  created, and an "account already exists" notice email is sent instead of a new
  verification token. The response never discloses whether the email existed.

Login and sessions:
- Login is by email and password.
- Email is normalized by trimming and lowercasing, consistently for registration,
  login, and email-specific invitations. Provider-specific normalization is not
  implemented.
- The session is carried in a session-scoped JSESSIONID cookie. Logout invalidates
  the server session and clears cookies.

CSRF:
- CSRF uses a cookie token (REVERIE-XSRF-TOKEN) that the client echoes as the
  X-REVERIE-XSRF-TOKEN header.
- The client initializes the cookie with a public GET /auth/csrf before the first
  mutating request, including login. The token is refreshed on login and cleared
  on logout. Initialization and refresh are covered by integration tests (see
  SECURITY.md), not assumed.

Account status and verification:
- enabled means the account is active. New accounts can authenticate before email
  verification.
- email_verified_at is a separate authorization gate. An authenticated but
  unverified user may call /auth/me, /auth/csrf, /auth/logout, /auth/verify-email,
  /auth/resend-verification, and the own-profile endpoints. Space, membership, and
  invitation endpoints return 403 until the email is verified.

## 3. Profiles

| Method | Endpoint | Description |
|---|---|---|
| GET | /users/me/profile | Get own profile |
| PUT | /users/me/profile | Update own profile |

The profile update accepts display name and optional bio. The username is set at
registration and is read-only in Milestone 1.

## 4. Spaces

| Method | Endpoint | Description |
|---|---|---|
| POST | /spaces | Create a space |
| GET | /spaces | List accessible spaces |
| GET | /spaces/{spaceId} | Get space details |
| PUT | /spaces/{spaceId} | Update space details |
| DELETE | /spaces/{spaceId} | Delete a space |
| GET | /spaces/{spaceId}/members | List members |
| DELETE | /spaces/{spaceId}/members/{userId} | Remove a member |

Space creation automatically creates an OWNER membership for the creator.

Spaces do not have a stored "type". Access is governed entirely by membership. A space
used by one person is simply a space with a single member; inviting someone adds a member.

## 5. Invitations

| Method | Endpoint | Description |
|---|---|---|
| POST | /spaces/{spaceId}/invitations | Create invitation (EMAIL or SHAREABLE) |
| GET | /spaces/{spaceId}/invitations | List invitations |
| GET | /invitations/preview?token=... | Public preview of an invitation by token |
| POST | /invitations/accept | Accept invitation |
| DELETE | /spaces/{spaceId}/invitations/{invitationId} | Revoke invitation |

Only the current OWNER of a space can create, list, or revoke invitations.

Invitation types and rules (see ADR-004):
- EMAIL invitations are single-use and bound to a specific invited email.
- SHAREABLE invitations are reusable with a usage limit (minimum 1, default 10,
  maximum 50). Default expiry is seven days. Terms are immutable after creation;
  to change them the owner revokes and recreates.
- Acceptance creates membership and consumes one use atomically. An existing
  member consumes no use and is not duplicated.

Preview and unavailable tokens:
- Preview returns the space name and inviter display name only when the invitation
  is valid.
- Every non-valid state (unknown, expired, revoked, exhausted) returns a uniform
  HTTP 404 that does not reveal which state applied. This applies to both preview
  and accept.
- Preview and accept are rate limited to blunt token brute-force.

## 6. Memories

| Method | Endpoint | Description |
|---|---|---|
| POST | /spaces/{spaceId}/memories | Create memory |
| GET | /spaces/{spaceId}/memories | List accessible memories |
| GET | /memories/{memoryId} | Get memory details |
| PUT | /memories/{memoryId} | Update memory |
| DELETE | /memories/{memoryId} | Delete memory |

Permissions:

- Only authorized space members can create memories.
- Only the memory creator can edit or delete it.
- Other authorized members can view SPACE_ONLY memories.
- PRIVATE memories are visible only to their creator.
- The backend must verify access before returning any memory.

## 7. Media

| Method | Endpoint | Description |
|---|---|---|
| POST | /memories/{memoryId}/media | Upload photograph |
| GET | /media/{mediaId} | Retrieve authorized media |
| DELETE | /media/{mediaId} | Delete own media |

All media access must be authorized through the associated memory.

## 8. Music

| Method | Endpoint | Description |
|---|---|---|
| POST | /memories/{memoryId}/songs | Associate song |
| GET | /memories/{memoryId}/songs | List songs |
| DELETE | /memories/{memoryId}/songs/{songId} | Remove association |

The MVP uses external music URLs.

## 9. Stories

| Method | Endpoint | Description |
|---|---|---|
| POST | /spaces/{spaceId}/stories | Create story |
| GET | /spaces/{spaceId}/stories | List accessible stories |
| GET | /stories/{storyId} | Get story |
| PUT | /stories/{storyId} | Update own story |
| DELETE | /stories/{storyId} | Delete own story |

Only the story creator can edit or delete their story.

A story associated with a PRIVATE memory must not expose its content to other space members.

## 10. Comments

| Method | Endpoint | Description |
|---|---|---|
| POST | /memories/{memoryId}/comments | Add comment |
| GET | /memories/{memoryId}/comments | List comments |
| PUT | /comments/{commentId} | Edit own comment |
| DELETE | /comments/{commentId} | Delete own comment |

Only users authorized to access the memory can comment.

Only the comment author can edit or delete their own comment.

## 11. Reactions

| Method | Endpoint | Description |
|---|---|---|
| PUT | /memories/{memoryId}/reaction | Create or update reaction |
| DELETE | /memories/{memoryId}/reaction | Remove reaction |

One reaction per user per memory.

## 12. Timeline

| Method | Endpoint | Description |
|---|---|---|
| GET | /timeline | Get accessible memories chronologically |

The timeline must never expose unauthorized content.

## 13. Error Response Format

Every failed request returns the same small, stable envelope. The implemented
shape is authoritative and has exactly four fields:

- `status`: the numeric HTTP status code.
- `code`: a stable machine-readable name for the error, such as `FORBIDDEN`,
  `NOT_FOUND`, `VALIDATION_FAILED`, `RATE_LIMITED`, or `INTERNAL_ERROR`.
- `message`: a short human-readable summary.
- `fieldErrors`: a list of field-level problems. It is empty for everything
  except validation failures.

The body never carries a timestamp, a request path, an exception type, a stack
trace, SQL, or any other internal detail. Those belong only in server logs (see
SECURITY.md section 10).

Example:

{
"status": 403,
"code": "FORBIDDEN",
"message": "You do not have permission to access this resource.",
"fieldErrors": []
}

For request validation failures, `fieldErrors` carries one entry per problem.
Each entry has a `field` (the request-body property path, or the parameter name
for a constrained request parameter or path variable) and a `message`:

{
"status": 400,
"code": "VALIDATION_FAILED",
"message": "Request validation failed.",
"fieldErrors": [
{ "field": "password", "message": "must be at least 10 characters" }
]
}

Status code policy:
- 400 for request validation failures, with fieldErrors.
- 401 when the request is not authenticated and the endpoint requires it.
- 403 when the authenticated user may see the resource but lacks permission for
  the specific action or role, including unverified-email access to protected
  space, membership, or invitation endpoints.
- 404 for visibility boundaries, where the user is not permitted to know the
  resource exists. This includes non-members of a private space and the uniform
  unavailable response for invitation tokens.
- 429 when a rate limit is exceeded, with a Retry-After header and a generic body.

The 404 visibility response must be uniform in body shape so that it does not
reveal whether a resource exists. Never expose internal stack traces, SQL
details, or other sensitive implementation details. Domain outcomes map to 4xx;
unexpected database or server errors map to a generic 500.

## 14. Pagination

Collection endpoints should support:

- page
- size
- sort

Example:

GET /api/v1/timeline?page=0&size=20&sort=eventDate,desc

Use sensible maximum page sizes.

## 15. API Design Rules

- Use DTOs instead of exposing persistence entities.
- Validate request bodies.
- Return appropriate HTTP status codes.
- Keep business logic in services.
- Avoid duplicated authorization logic.
- Document endpoints using OpenAPI.
- Do not expose password hashes or invitation token hashes.
- Do not trust resource identifiers without checking authorization.

## 16. Deferred API Features

- Public discovery.
- Advanced search.
- Notifications.
- AI endpoints.
- Music provider integrations.
- Analytics.
