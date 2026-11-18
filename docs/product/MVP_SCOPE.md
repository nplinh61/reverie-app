
# Reverie - MVP Scope

## Objective

Build a usable first version of Reverie that allows individuals and small groups to preserve and revisit meaningful moments through memories, photographs, music, and stories.

## Must Have (P0)

- User registration and authentication.
- User profiles.
- Create and manage spaces (used personally or shared with invited members).
- Invite users through invitation links.
- Space ownership and membership.
- Create, edit, view, and delete memories.
- Attach photographs to memories.
- Attach external music links.
- Write and view stories.
- Chronological memory timeline.
- Private and space-only visibility.
- Backend authorization.
- Basic comments and reactions.

## Should Have (P1)

- Search memories.
- Filter memories by date.
- Better profile customization.
- Improved mobile interactions.
- Memory cover images.
- Basic notification experience.

These features should only be considered after the core journey works.

## Won't Have in MVP

- Native music streaming.
- Spotify or Apple Music API integration.
- AI features.
- Public social feed.
- Advanced discovery.
- Real-time collaboration.
- Payments or subscriptions.
- Complex analytics.
- Account deletion and shared-content ownership transfer.
- Public profiles, public memories, and public discovery.

## MVP Success Criteria

The MVP is ready for initial validation when:

1. A user can register and log in.
2. A user can create a space.
3. A second user can join through an invitation.
4. Both users can access authorized shared memories.
5. A memory can contain a photograph, song link, and story.
6. Members can comment on memories.
7. Unauthorized users cannot access private content.
8. The application works on web and a mobile device.

### Memory Visibility in the MVP

The MVP supports only two visibility levels:

- PRIVATE: Only the memory creator can access the memory.
- SPACE_ONLY: The creator and authorized members of the associated space can access the memory.

PUBLIC visibility, public profiles, and public discovery are deferred until a future release.

A private space must never expose its content publicly.
Stories attached to a memory inherit that memory's visibility.


## Implementation Priority

Build one complete end-to-end journey before expanding functionality.

Do not implement all features in parallel.
