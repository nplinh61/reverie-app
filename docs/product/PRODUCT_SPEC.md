
# Reverie - Product Specification

Version: 1.0
Status: Initial product baseline
Product name: Reverie

## 1. Product Overview

### 1.1 Vision

Reverie is a personal music and memory platform that allows people to preserve meaningful moments through memories, music, photographs, and stories.

It should feel like a personal collection of moments rather than a conventional social network.

Users can create private memories, share spaces with friends or partners, and revisit their shared experiences over time.

### 1.2 Target Audience

- Individuals who want to preserve personal memories.
- Couples who want to document their relationship and shared experiences.
- Friends who want to collect memories together.

### 1.3 Core Product Concept

Reverie combines three equally important elements:

**Memories**
- Moments, events, and experiences.
- Dates, descriptions, and photographs.

**Music**
- Songs associated with specific memories.
- External music links for the MVP.
- Song title, artist, and optional cover information.

**Stories**
- Personal writing that gives memories context.
- Longer reflections and narratives.
- Stories associated with memories or spaces.

## 2. Product Principles

1. Personal expression comes before social engagement.
2. Memories, music, and stories have equal importance.
3. Privacy must be clear and understandable.
4. Users control who can access their content.
5. Shared spaces should feel collaborative and personal.
6. The interface should be warm, calm, and emotionally engaging.
7. The MVP should prioritize reliability over feature quantity.

## 3. Platforms

The initial product targets:

- Web application.
- Mobile application for iOS and Android.

The frontend should share as much code as reasonably possible across platforms.

## 4. Main Navigation

The initial navigation consists of:

### Home
- Overview of recent memories.
- Recently updated shared spaces.
- Quick access to creating a memory.

### Memories
- Browse personal and accessible memories.
- Chronological timeline.
- View memory details.

### Soundtracks
- Browse songs associated with memories.
- Discover personal music collections.
- Open external music links.

### Spaces
- View your spaces.
- Create spaces.
- Invite other users.
- Manage membership.

### Profile
- Personal information.
- Profile settings.
- Optional public profile and discovery settings.

## 5. Core Functional Requirements

### 5.1 Authentication and Identity

FR-01: Users can register an account.

FR-02: Users can securely log in and log out.

FR-03: Users can manage basic profile information.

FR-04: Users can update account settings.

### 5.2 Spaces and Membership

FR-05: Users can create spaces that are accessible only to their members.

FR-06: Users can invite other users through invitation links.

FR-07: Users can join a space using a valid invitation.

FR-08: Space owners can manage membership.

FR-09: Spaces support at least two roles:
- OWNER
- MEMBER

FR-10: Users can view only spaces they are authorized to access.

### 5.3 Memories

FR-11: Users can create memories within a space they are authorized to access.

FR-12: Memories contain:
- Title.
- Description.
- Date of the event.
- Creation timestamp.
- Optional photographs.
- Visibility setting.

FR-13: Users can edit and delete their own memories, subject to space permissions.

FR-14: Users can browse memories chronologically.

FR-15: Users can open a detailed memory view.

### 5.4 Music

FR-16: Users can associate songs with memories.

FR-17: Songs can be added using external music URLs.

FR-18: Song information can include:
- Title.
- Artist.
- External URL.
- Optional cover image.
- Optional platform information.

FR-19: Users can open the associated music link externally.

### 5.5 Stories

FR-20: Users can write stories associated with a memory or space.

FR-21: Stories support a title and longer text content.

FR-22: Users can edit and delete their own stories, subject to permissions.

### 5.6 Social Interaction

FR-23: Authorized members can comment on memories.

FR-24: Authorized members can react to memories.

FR-25: Users can revisit and interact with content in their shared spaces.

## 6. Privacy and Visibility

Reverie must support three visibility levels:

### PRIVATE
Only the content owner can access the content, subject to applicable account and space rules.

### SPACE_ONLY
Only authorized members of the associated space can access the content.

### PUBLIC
Content may be accessible to other users through an explicitly enabled public profile or discovery feature.

Important privacy rules:

- A private space must never expose its content publicly.
- Marking an individual memory as PUBLIC must not override the privacy of its private space.
- Backend authorization is mandatory for every protected operation.
- Unauthorized users must not access private media through direct URLs.
- Public discovery must be explicitly enabled and must respect the visibility of the containing space.
- Users must understand who can see their content before publishing it.

### Shared Memory Ownership and Editing Permissions

- The user who creates a memory is its owner.
- Only the creator can edit or delete their memory.
- Other members of the same shared space can view SPACE_ONLY memories.
- Other members can comment on and react to accessible memories.
- Space ownership does not automatically grant permission to edit or delete another member's memories.
- Any future moderation or exceptional deletion functionality requires a separate explicit permission model.


## 7. User Stories and Acceptance Criteria

The detailed user stories and their acceptance criteria are maintained in a single
canonical document: `docs/product/USER_STORIES.md`.

That document is the source of truth. Keep it updated instead of duplicating the
stories here.

## 8. Non-Functional Requirements

### Security
- Secure authentication.
- Password hashing.
- Backend authorization.
- Input validation.
- Protection against common web vulnerabilities.
- Secure media access.

### Privacy
- Explicit visibility settings.
- No unauthorized access to private content.
- Secure invitation handling.
- Clear ownership and access rules.

### Performance
- Responsive user interface.
- Pagination for growing memory collections.
- Efficient database queries.
- Avoid unnecessary media downloads.

### Reliability
- Consistent database transactions.
- Database migrations.
- Meaningful error handling.
- Automated tests for critical functionality.

### Usability
- Responsive web and mobile experience.
- Accessible interface components.
- Clear loading, empty, and error states.
- Consistent visual language.

## 9. MVP Scope

The first MVP should include:

- Account registration and authentication.
- User profiles.
- Spaces with membership and invitations.
- Invitation links.
- Space membership and roles.
- Memory creation and management.
- Photograph uploads.
- Songs through external links.
- Stories.
- Chronological timeline.
- Private and space-only visibility.
- Comments and reactions.

Public discovery is not required for the first MVP. The data model may support future expansion, but it should not delay the core product.

## 10. Deferred Features

These features are intentionally excluded from the initial MVP:

- Spotify or Apple Music API integration.
- AI-generated stories.
- AI-generated memory summaries.
- AI-powered recommendations.
- Collaborative real-time editing.
- Advanced social discovery.
- Public feeds.
- Push notifications.
- Complex analytics and dashboards.
- Subscription and payment systems.

## 11. MVP Validation

The MVP should be tested with approximately 5-10 initial users.

Validation goals:
- Can users understand the concept without extensive explanation?
- Can they create a memory easily?
- Can they invite another person?
- Do shared spaces feel meaningful?
- Do users return to revisit their memories?
- Do users trust the privacy controls?

## 12. First End-to-End User Journey

The first complete vertical slice should demonstrate:

1. User A registers.
2. User A creates a space.
3. User A invites User B.
4. User B joins the space.
5. User A creates a memory.
6. User A attaches a photograph.
7. User A adds a song link and a story.
8. User B views the memory.
9. User B comments on it.
10. Both users can revisit the memory later.

This journey is the primary reference for early implementation priorities.
