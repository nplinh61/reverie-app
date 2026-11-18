# Reverie - User Stories and Acceptance Criteria

Status: Canonical source
Product name: Reverie

This document is the single source of truth for Reverie's user stories and their
acceptance criteria. Other documents (for example `PRODUCT_SPEC.md`) reference this
file instead of repeating the stories, so there is only one place to keep updated.

Scope note: these stories describe the MVP. Memory visibility in the MVP is limited
to `PRIVATE` and `SPACE_ONLY`. Public profiles, public memories, and public discovery
are deferred.

Space model note: a space is accessible only to its members. The creator is
automatically the first member, with the role `OWNER`. A space used by a single
person is simply a space with one member; inviting someone adds another member.
There is no stored "personal" or "shared" space type.

## US-01: Account Registration

As a new user, I want to register so that I can use Reverie.

Acceptance criteria:
- Registration requires valid account information.
- Duplicate accounts are rejected.
- Passwords are stored securely.
- Successful registration creates a user account.

## US-02: Create a Space

As a user, I want to create a space so that I can organize memories.

Acceptance criteria:
- A user can create a space with a name.
- The creator becomes its OWNER and first member.
- The space is accessible only to its members.
- Unauthorized users cannot access it.

## US-03: Invite Someone

As a space owner, I want to invite another person so that we can share memories.

Acceptance criteria:
- Only the current OWNER of a space can create or revoke invitations for that space.
- Ordinary space members cannot create, list, or revoke invitations.
- Invitations have unguessable tokens.
- Invitations can expire, and the owner can revoke an existing invitation.
- A valid invitation allows the invited user to join as a member.
- Invalid or expired invitations are rejected.

## US-04: Create a Memory

As a user, I want to create a memory so that I can preserve a meaningful moment.

Acceptance criteria:
- A memory has a title and date.
- A description is optional.
- A memory belongs to a space the creator is authorized to access.
- Only the memory creator can edit or delete their memory.
- Being a space OWNER does not automatically grant permission to edit or delete another member's memory.
- Space membership determines access, not ownership.

## US-05: Add a Photograph

As a user, I want to attach photographs to a memory so that I can preserve its visual context.

Acceptance criteria:
- A memory can have optional photographs.
- Supported file types and sizes are validated.
- Unauthorized users cannot access private photographs.

## US-06: Add a Song

As a user, I want to associate a song with a memory so that I can remember the music connected to that moment.

Acceptance criteria:
- A song can be added through an external URL.
- The song is associated with the selected memory.
- Users can open the external link.

## US-07: Write a Story

As a user, I want to write a story so that I can describe the meaning behind a memory.

Acceptance criteria:
- A story has a title and text.
- A story can be associated with a memory or space.
- Authorized users can read it.
- Only the story author can edit or delete their own story, subject to applicable access rules.

## US-08: Browse a Timeline

As a user, I want to browse memories chronologically so that I can revisit the past.

Acceptance criteria:
- Memories are displayed in chronological order.
- The user only sees content they can access.
- A memory can be opened from the timeline.

## US-09: Comment on a Memory

As a space member, I want to comment on a memory so that I can share my thoughts.

Acceptance criteria:
- Authorized members can add comments.
- Comments belong to a memory and an author.
- Authors can edit or delete their own comments.
- Unauthorized users cannot read or create comments.

## US-10: React to a Memory

As a space member, I want to react to a memory so that I can express my feelings.

Acceptance criteria:
- Authorized members can react.
- A reaction is associated with a user and memory.
- Users can change or remove their reaction.

## US-11: Manage My Profile

As a user, I want to manage my profile so that I can personalize my presence in Reverie.

Acceptance criteria:
- Users can update basic profile information.
- Private content remains inaccessible regardless of profile visibility.
