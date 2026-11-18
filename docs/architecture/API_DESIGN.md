
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
| GET | /auth/me | Get current user |

Authentication must use the approved secure session/token strategy.

## 3. Profiles

| Method | Endpoint | Description |
|---|---|---|
| GET | /users/me/profile | Get own profile |
| PUT | /users/me/profile | Update own profile |

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
| POST | /spaces/{spaceId}/invitations | Create invitation |
| GET | /spaces/{spaceId}/invitations | List invitations |
| POST | /invitations/accept | Accept invitation |
| DELETE | /spaces/{spaceId}/invitations/{invitationId} | Revoke invitation |

Only authorized space owners can manage invitations.

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

Use a consistent error structure.

Example:

{
"timestamp": "2026-11-10T12:00:00Z",
"status": 403,
"error": "FORBIDDEN",
"message": "You do not have permission to access this resource.",
"path": "/api/v1/memories/example-id"
}

Never expose internal stack traces or sensitive implementation details.

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
