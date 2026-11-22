
# Reverie - Database Design

Version: 1.0
Status: Proposed database baseline
Database: PostgreSQL

## 1. Design Principles

- Use UUID primary keys.
- Use PostgreSQL.
- Use Flyway for database migrations.
- Use foreign keys to enforce relationships.
- Use NOT NULL constraints for required fields.
- Use unique constraints where appropriate.
- Store timestamps in UTC.
- Use DATE for the date of a memory event.
- Keep uploaded binary files outside PostgreSQL.
- Enforce authorization in the backend.

This document describes the proposed MVP schema. It must be reviewed before implementation.

## 2. Entity Overview

The MVP uses the following tables:

1. users
2. profiles
3. email_verification_tokens
4. spaces
5. space_members
6. invitations
7. invitation_acceptances
8. memories
9. media
10. songs
11. memory_songs
12. stories
13. comments
14. reactions

Tables 1 to 7 are implemented in Milestone 1. Tables 8 to 14 belong to Milestone
2 and later. The section numbers below are historical and are not renumbered when
a table is inserted; email_verification_tokens appears as 3.1.1 and
invitation_acceptances as 3.5.1.

## 3. Table Definitions

### 3.1 users

Stores authentication and account information.

| Column | Type | Constraints |
|---|---|---|
| id | UUID | Primary key |
| email | VARCHAR(255) | NOT NULL, UNIQUE |
| password_hash | VARCHAR(255) | NOT NULL |
| email_verified_at | TIMESTAMPTZ | NULL |
| enabled | BOOLEAN | NOT NULL, DEFAULT TRUE |
| created_at | TIMESTAMPTZ | NOT NULL |
| updated_at | TIMESTAMPTZ | NOT NULL |

Rules:
- Email addresses are normalized before storage by trimming and lowercasing. The
  same normalization is applied for login and for email-specific invitation
  matching. Provider-specific normalization is not implemented. Because stored
  emails are already normalized, the UNIQUE constraint enforces case-insensitive
  uniqueness in practice.
- Passwords must be hashed using a suitable password hashing algorithm. Milestone
  1 uses BCrypt at cost 12 through a DelegatingPasswordEncoder (see ADR-003).
- Never store plaintext passwords.
- Authentication secrets must never be exposed through API responses.
- enabled means the account is active. It defaults to true, and a new account can
  authenticate before verifying its email. enabled is not the verification flag.
- email_verified_at is null until the user verifies their email. It is a separate
  authorization gate, not an authentication flag. An unverified user cannot
  participate in protected spaces, membership, or invitations (see ADR-005).

### 3.1.1 email_verification_tokens

Stores single-use tokens for email verification. Added in Milestone 1.

| Column | Type | Constraints |
|---|---|---|
| id | UUID | Primary key |
| user_id | UUID | NOT NULL, FK users.id |
| token_hash | VARCHAR(255) | NOT NULL, UNIQUE |
| expires_at | TIMESTAMPTZ | NOT NULL |
| consumed_at | TIMESTAMPTZ | NULL |
| created_at | TIMESTAMPTZ | NOT NULL |

Rules:
- The raw token is delivered only in the verification email. Only its SHA-256 hash
  is stored. A fast cryptographic hash is appropriate because the token is high
  entropy, unlike a password.
- Tokens are single-use and expire after 24 hours.
- A resend invalidates prior unconsumed tokens for the same user.
- Consumption is atomic: a conditional UPDATE sets consumed_at only when the token
  is unconsumed and unexpired, and the same transaction sets users.email_verified_at
  (see ADR-005).
- Tokens must never be returned through API responses or written to logs.

### 3.2 profiles

Stores user-facing profile information.

| Column | Type | Constraints |
|---|---|---|
| id | UUID | Primary key |
| user_id | UUID | NOT NULL, UNIQUE, FK users.id |
| username | VARCHAR(30) | NOT NULL, UNIQUE (case-insensitive) |
| display_name | VARCHAR(100) | NOT NULL |
| bio | VARCHAR(500) | NULL |
| avatar_media_id | UUID | NULL, FK media.id |
| created_at | TIMESTAMPTZ | NOT NULL |
| updated_at | TIMESTAMPTZ | NOT NULL |

A user has exactly one profile.

Username rules:
- Length 3 to 30 characters, lowercase ASCII letters, digits, and underscores.
- Must contain at least one alphanumeric character. Leading and trailing
  underscores are acceptable.
- Uniqueness is case-insensitive, enforced by a unique index on lower(username).
- The UUID is the permanent internal identifier and the foreign-key target.
  Username is a mutable attribute, but username changes are deferred beyond
  Milestone 1 and the username is read-only after registration in Milestone 1.

The backend must verify that any media referenced by avatar_media_id has purpose PROFILE_AVATAR and was uploaded by the same user as the profile.

Migration ordering note: avatar_media_id references media.id, but the media table is
created after profiles. To avoid an impossible forward reference, the avatar_media_id
column is created as a nullable UUID first, and its foreign-key constraint is added in a
later migration (in or after the migration that creates the media table) using
ALTER TABLE. See section 8 for the general convention.

Public profile discovery is deferred.

### 3.3 spaces

Represents a collection of memories, stories, and related content.

| Column | Type | Constraints |
|---|---|---|
| id | UUID | Primary key |
| name | VARCHAR(100) | NOT NULL |
| description | VARCHAR(500) | NULL |
| created_by_user_id | UUID | NOT NULL, FK users.id |
| created_at | TIMESTAMPTZ | NOT NULL |
| updated_at | TIMESTAMPTZ | NOT NULL |

Rules:
- A space is accessible only to its members.
- The creator is automatically added as the OWNER member when the space is created.
- A space used by a single person is simply a space with one member.
- There is no stored space type. Inviting someone adds another member rather than
  changing a space from one type to another.
- A space cannot be accessed by unauthorized users.
- Space creation must also create the creator's membership record in the same transaction.

### 3.4 space_members

Connects users to spaces and defines their roles.

| Column | Type | Constraints |
|---|---|---|
| id | UUID | Primary key |
| space_id | UUID | NOT NULL, FK spaces.id |
| user_id | UUID | NOT NULL, FK users.id |
| role | VARCHAR(20) | NOT NULL |
| joined_at | TIMESTAMPTZ | NOT NULL |

Supported roles:
- OWNER
- MEMBER

Constraints:
- UNIQUE(space_id, user_id)
- A space must have exactly one active OWNER.
- The OWNER is also a member of the space.
- Only authorized members can access a space's content.

The database should enforce at most one OWNER per space using an appropriate partial unique index.

The application must ensure that every space has an OWNER and that ownership changes are transactional.

A space always retains an OWNER. In Milestone 1 an owner cannot leave or remove
themselves, and ownership transfer is deferred. Member removal must reject
removing the sole OWNER.

### 3.5 invitations

Stores invitations to spaces.

| Column | Type | Constraints |
|---|---|---|
| id | UUID | Primary key |
| space_id | UUID | NOT NULL, FK spaces.id |
| created_by_user_id | UUID | NOT NULL, FK users.id |
| type | VARCHAR(20) | NOT NULL |
| invited_email | VARCHAR(255) | NULL |
| token_hash | VARCHAR(255) | NOT NULL, UNIQUE |
| max_uses | INTEGER | NOT NULL |
| uses_consumed | INTEGER | NOT NULL, DEFAULT 0 |
| expires_at | TIMESTAMPTZ | NOT NULL |
| revoked_at | TIMESTAMPTZ | NULL |
| created_at | TIMESTAMPTZ | NOT NULL |

Supported types:
- EMAIL: single-use, bound to a specific invited email. max_uses is 1.
- SHAREABLE: a reusable link with an owner-configured usage limit. Minimum 1,
  default 10, maximum 50.

Status is derived from stored facts, not stored as a column. Precedence:
- REVOKED if revoked_at is set
- EXPIRED if now is past expires_at
- EXHAUSTED if uses_consumed is at least max_uses
- otherwise ACTIVE

Database constraints:

```sql
CONSTRAINT chk_invitation_type_email
CHECK (
    (type = 'EMAIL' AND invited_email IS NOT NULL AND max_uses = 1)
    OR
    (type = 'SHAREABLE' AND invited_email IS NULL AND max_uses BETWEEN 1 AND 50)
)
CONSTRAINT chk_invitation_uses
CHECK (uses_consumed >= 0 AND uses_consumed <= max_uses)
```

Rules:
- Store only a SHA-256 hash of the invitation token. Generate unguessable tokens
  with a cryptographically secure random generator.
- Invitation terms are immutable after creation. To change them the owner revokes
  and recreates.
- Invitations can be revoked. Revocation blocks future use but does not remove
  existing members.
- Expired, revoked, exhausted, or already-consumed invitations cannot grant
  membership.
- Accepting an invitation creates a space_members record and consumes one use,
  atomically, in a single transaction (see ADR-004).
- An existing member who accepts again consumes no use and creates no duplicate
  membership.
- Only the current OWNER of a space may create, list, or revoke invitations for
  that space. Space membership alone does not grant invitation-management
  permissions.
- Unavailable tokens return a uniform response that does not reveal whether the
  token was unknown, expired, revoked, or exhausted.

### 3.5.1 invitation_acceptances

Records each acceptance of an invitation. Added in Milestone 1. Supports the usage
limit and per-user uniqueness for reusable links.

| Column | Type | Constraints |
|---|---|---|
| id | UUID | Primary key |
| invitation_id | UUID | NOT NULL, FK invitations.id |
| user_id | UUID | NOT NULL, FK users.id |
| space_member_id | UUID | NOT NULL, FK space_members.id |
| accepted_at | TIMESTAMPTZ | NOT NULL |

Constraints:
- UNIQUE(invitation_id, user_id)

Rules:
- An acceptance row exists only when that acceptance actually created the
  membership.
- Acceptance uses INSERT ... ON CONFLICT DO NOTHING and conditional SQL rather
  than catching unique-constraint violations, so the transaction is never aborted
  by a constraint error (see ADR-004).

### 3.6 memories

Stores individual moments.

| Column | Type | Constraints |
|---|---|---|
| id | UUID | Primary key |
| space_id | UUID | NOT NULL, FK spaces.id |
| created_by_user_id | UUID | NOT NULL, FK users.id |
| title | VARCHAR(200) | NOT NULL |
| description | TEXT | NULL |
| event_date | DATE | NOT NULL |
| visibility | VARCHAR(20) | NOT NULL |
| created_at | TIMESTAMPTZ | NOT NULL |
| updated_at | TIMESTAMPTZ | NOT NULL |

Supported visibility:
- PRIVATE
- SPACE_ONLY

Rules:
- Every memory belongs to exactly one space.
- The creator must have permission to create memories in that space.
- PRIVATE memories are visible only to their creator.
- SPACE_ONLY memories are visible to authorized space members.
- The containing space's access restrictions always apply.
- A memory must never grant access to a space that a user cannot access.
- Only created_by_user_id can edit or delete a memory.
- Space membership grants access, not ownership.
- Space owners cannot automatically edit or delete memories created by other members.
- Comments and reactions are separate resources with their own permissions.

### 3.7 media

Stores metadata for uploaded files. Actual binary files are stored in object storage.

| Column | Type | Constraints |
|---|---|---|
| id | UUID | Primary key |
| uploaded_by_user_id | UUID | NOT NULL, FK users.id |
| memory_id | UUID | NULL, FK memories.id |
| purpose | VARCHAR(30) | NOT NULL |
| storage_key | VARCHAR(1024) | NOT NULL, UNIQUE |
| original_filename | VARCHAR(255) | NOT NULL |
| content_type | VARCHAR(100) | NOT NULL |
| file_size | BIGINT | NOT NULL |
| created_at | TIMESTAMPTZ | NOT NULL |

Supported purposes:
- MEMORY_PHOTO
- PROFILE_AVATAR

Rules:
- Actual binary files are stored in object storage.
- storage_key is a private storage reference, not a public URL.
- Validate file size and file type on the backend.
- Never trust the original filename or client-provided MIME type.
- Access to memory photographs must be checked against the associated memory's permissions.
- Profile avatars must be owned and managed by the associated user.
- The backend must verify that the user setting an avatar is the owner of the referenced avatar media.
- Deleting a memory must not leave accessible orphaned media.

Database constraint:

The database must enforce the relationship between purpose and memory_id with a CHECK constraint:

```sql
CONSTRAINT chk_media_purpose_memory
CHECK (
    (purpose = 'MEMORY_PHOTO' AND memory_id IS NOT NULL)
    OR
    (purpose = 'PROFILE_AVATAR' AND memory_id IS NULL)
)
```

Every media record must have an uploader. Memory photographs must follow the access permissions of their associated memory.
### 3.8 songs

Stores external music information.

| Column | Type | Constraints |
|---|---|---|
| id | UUID | Primary key |
| title | VARCHAR(255) | NOT NULL |
| artist | VARCHAR(255) | NOT NULL |
| external_url | VARCHAR(2048) | NOT NULL |
| platform | VARCHAR(50) | NULL |
| cover_image_url | VARCHAR(2048) | NULL |
| created_at | TIMESTAMPTZ | NOT NULL |

Rules:
- Music is linked externally.
- Reverie does not host or stream music.
- Validate URL schemes and supported external platforms.
- Do not automatically download copyrighted music or cover images.

### 3.9 memory_songs

Associates songs with memories.

| Column | Type | Constraints |
|---|---|---|
| id | UUID | Primary key |
| memory_id | UUID | NOT NULL, FK memories.id |
| song_id | UUID | NOT NULL, FK songs.id |
| position | INTEGER | NOT NULL, DEFAULT 0 |
| added_by_user_id | UUID | NOT NULL, FK users.id |
| created_at | TIMESTAMPTZ | NOT NULL |

Constraints:
- UNIQUE(memory_id, song_id)
- A memory can contain multiple songs.
- A song can be associated with multiple memories.

### 3.10 stories

Stores longer written content.

| Column | Type | Constraints |
|---|---|---|
| id | UUID | Primary key |
| space_id | UUID | NOT NULL, FK spaces.id |
| memory_id | UUID | NULL, FK memories.id |
| created_by_user_id | UUID | NOT NULL, FK users.id |
| title | VARCHAR(200) | NOT NULL |
| content | TEXT | NOT NULL |
| created_at | TIMESTAMPTZ | NOT NULL |
| updated_at | TIMESTAMPTZ | NOT NULL |

Rules:
- Every story belongs to a space.
- A story may optionally belong to a memory.
- If memory_id is present, the memory must belong to the same space.
- Access to a story follows the space and associated memory's privacy rules.
- Stories attached to memories inherit the visibility of their associated memory.
- Stories without an associated memory are accessible only to authorized members of their space.
- A story must never be more publicly accessible than its associated memory.
- Only the story creator can edit or delete a story in the MVP.
- Other authorized space members can view accessible stories.

### 3.11 comments

Stores comments on memories.

| Column | Type | Constraints |
|---|---|---|
| id | UUID | Primary key |
| memory_id | UUID | NOT NULL, FK memories.id |
| created_by_user_id | UUID | NOT NULL, FK users.id |
| content | TEXT | NOT NULL |
| created_at | TIMESTAMPTZ | NOT NULL |
| updated_at | TIMESTAMPTZ | NOT NULL |

Rules:
- Only users authorized to access a memory can comment.
- Validate comment content.
- Comments must inherit the access restrictions of their memory.
- Only the comment author can edit or delete their own comment.

### 3.12 reactions

Stores reactions to memories.

| Column | Type | Constraints |
|---|---|---|
| id | UUID | Primary key |
| memory_id | UUID | NOT NULL, FK memories.id |
| user_id | UUID | NOT NULL, FK users.id |
| reaction_type | VARCHAR(30) | NOT NULL |
| created_at | TIMESTAMPTZ | NOT NULL |

Constraints:
- UNIQUE(memory_id, user_id)

Rules:
- One reaction per user per memory.
- A user can update or remove their reaction.
- Only authorized users can react.

## 4. Important Relationships

- users 1:1 profiles
- users 1:N email_verification_tokens
- users 1:N spaces (creator relationship)
- users N:M spaces through space_members
- spaces 1:N invitations
- invitations 1:N invitation_acceptances
- spaces 1:N memories
- users 1:N memories (creator relationship)
- memories 1:N media
- memories N:M songs through memory_songs
- spaces 1:N stories
- memories 0:N stories
- memories 1:N comments
- memories 1:N reactions

## 5. Database Constraints and Indexes

Recommended indexes:

- users(email)
- profiles unique index on lower(username)
- email_verification_tokens(user_id)
- space_members(user_id, space_id)
- invitations(token_hash)
- invitations(space_id)
- memories(space_id, event_date)
- memories(created_by_user_id)
- media(memory_id)
- memory_songs(memory_id)
- stories(space_id)
- stories(memory_id)
- comments(memory_id, created_at)
- reactions(memory_id)

Add database constraints for:
- Unique email addresses.
- Unique username, case-insensitive, through a unique index on lower(username).
- At most one OWNER per space, through a partial unique index.
- Unique space membership.
- Unique invitation token hashes.
- Unique acceptance per user per invitation.
- Valid invitation type with its invited_email and max_uses combination, and a
  uses_consumed range within max_uses.
- Unique song association per memory.
- Unique reaction per user and memory.
- Valid enum-like values.
- Positive media file sizes.
- Non-negative song positions.

Use appropriate CHECK constraints and foreign-key deletion behavior.

## 6. Privacy and Authorization Rules

The backend must check permissions before retrieving or modifying protected resources.

Important scenarios:

1. A user cannot access a space they are not a member of.
2. A non-member cannot access any space.
3. A member cannot access another member's PRIVATE memory.
4. A member can access SPACE_ONLY memories in a space they belong to.
5. A user cannot access private photographs through direct storage references.
6. A user cannot comment on or react to inaccessible memories.
7. A revoked invitation cannot be accepted.
8. A story associated with a PRIVATE memory must not expose that memory's content to other space members.

Do not rely only on frontend visibility controls.

## 7. Deletion Strategy

Define explicit deletion behavior before implementing destructive operations.

Suggested initial policy:

- Deleting a user should not automatically delete shared content without an ownership-transfer policy.
- Deleting a space requires handling its memories, stories, invitations, and media.
- Deleting a memory requires handling its media, songs associations, comments, reactions, and associated stories.
- Deleting a media record should trigger controlled deletion of its object-storage file.
- Avoid cascading deletion of shared data without explicit product rules.

Use transactions and an appropriate cleanup strategy for object storage.

## 8. Migration Strategy

Use Flyway.

Milestone 0 ships only a minimal baseline migration. Its purpose is to prove that
Flyway, PostgreSQL, and the backend are wired together correctly. It does not create
the domain tables. A suitable baseline enables the UUID extension or creates a small
disposable sanity object.

Milestone 0 contains exactly one migration: V1__baseline.sql. No domain tables are
created in Milestone 0.

The real domain tables begin in Milestone 1. V2 and every later migration belong to
Milestone 1 or a subsequent milestone, and are added milestone by milestone.

Cross-cutting foreign-key convention: create tables first, then add foreign keys that
point to a table created in a later migration using ALTER TABLE in (or after) the
migration that creates the referenced table. This avoids impossible forward references,
such as profiles.avatar_media_id pointing at media.id (see section 3.2).

The Milestone 1 sequence is fixed below. Names and boundaries beyond Milestone 1
remain illustrative and will change during implementation.

```text
apps/api/src/main/resources/db/migration/

# Milestone 0 (foundation): the only migration shipped in this milestone
├── V1__baseline.sql                      (enable UUID extension / sanity object; no domain tables)

# Milestone 1 (accounts and spaces)
├── V2__create_users.sql
├── V3__create_profiles.sql               (unique index on lower(username); avatar_media_id nullable, no FK yet)
├── V4__create_email_verification_tokens.sql
├── V5__create_spaces.sql
├── V6__create_space_members.sql          (UNIQUE(space_id,user_id); partial unique OWNER index)
├── V7__create_invitations.sql            (type, max_uses, uses_consumed, token_hash unique, CHECK constraints)
├── V8__create_invitation_acceptances.sql (UNIQUE(invitation_id,user_id))

# Milestone 2 and later (domain tables): added milestone by milestone
├── V9__create_memories_and_media.sql     (adds the profiles.avatar_media_id FK via ALTER TABLE)
├── ...
```

Migration names and boundaries beyond V1 may change during implementation.

Never modify an already-applied migration in a shared or persistent environment. Create a new migration instead.

## 9. Open Design Questions

These questions must be resolved before the corresponding features are implemented:

1. Can multiple people edit a shared memory?
2. Can a space owner transfer ownership? Deferred beyond Milestone 1. In Milestone
   1 the owner is fixed and cannot leave or remove themselves.
3. Should memories support soft deletion?
4. Should stories have their own visibility setting?
5. Should invitations be email-specific or shareable by anyone with the link?
   Resolved: both. Milestone 1 supports EMAIL (single-use) and SHAREABLE
   (usage-limited reusable) invitations (see section 3.5 and ADR-004).
6. What should happen to shared content when a user deletes their account?

Do not silently invent policies for these cases.