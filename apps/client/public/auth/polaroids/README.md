# Registration polaroid photo slots

The registration screen shows decorative polaroids. Until licensed photographs
are supplied, each polaroid renders a polished gradient placeholder, so the page
looks complete with **no image files present** and makes **no failed image
requests** (the components do not reference missing files).

To use real photography later, drop appropriately licensed JPG/WEBP files here
with these exact names and set the matching `--photo` flag on the component (see
`register.ts` `polaroids`), without changing the layout:

| File name            | Used on  | Suggested subject (per approved mockups) |
|----------------------|----------|------------------------------------------|
| `friends-sunset.jpg` | desktop  | Friends watching a sunset ("late summer nights") |
| `record-player.jpg`  | desktop  | Flowers by a record player ("the song that takes me back") |
| `concert.jpg`        | desktop  | A live concert crowd ("our first concert") |
| `that-summer.jpg`    | mobile   | Friends by the water ("that summer") |

Recommended: ~4:5 portrait crops, at least 600x750 px, optimized for web. Only
add images you have the rights to use. Do not commit unlicensed photographs.
