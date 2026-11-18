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

## 8. Account and Data Lifecycle

- Account deletion behavior must be explicitly designed before implementation.
- Deleting an account must not silently delete or expose shared content belonging to other users.
- The MVP must not implement destructive account deletion without a documented policy.
- Data retention and cleanup behavior for expired invitations and orphaned media must be documented.
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

## 12. Security Development Principles

- Security requirements must be considered during design, not added only after implementation.
- Backend authorization must be tested independently of frontend behavior.
- New endpoints must define their authentication and authorization requirements.
- Changes to privacy rules must be reflected in the product specification, API design, database design, and tests.
- The MVP should prefer simple, explicit security rules over premature abstractions.
