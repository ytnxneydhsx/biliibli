# Homepage Refresh Design

**Date:** 2026-04-09

**Goal:** Rework the `/` homepage of `bilibili_web` into a more beautiful, youthfully energetic, video-first landing page without introducing any frontend capability that is not backed by existing backend APIs and routes.

## Product Intent

This homepage refresh should make the site feel closer to a content platform than a CRUD demo. The page should help a first-time visitor immediately understand that they can watch videos, browse hot content, and continue exploring through real working routes.

The chosen direction is:

- Visual tone: youthfully energetic
- Homepage emphasis: watch videos first
- Page strategy: content-led layout

## Existing Constraints

The design must respect the current codebase and backend contract.

### Frontend Context

- Project: `bilibili_web`
- Stack: Vue 3 + TypeScript + Vite + Vue Router
- Current homepage already loads:
  - `/videos` for public video list
  - `/videos/rank` for ranking list
- Existing usable routes include:
  - `/`
  - `/search`
  - `/video/:id`
  - `/studio`

### Hard Functional Boundary

The redesign must not add fake actions, fake tabs, fake categories, fake filters, fake recommendation systems, or fake social widgets that imply missing backend behavior.

Only these homepage interactions are allowed in the first iteration:

- Navigate to a video detail page using real video IDs
- Navigate to the search page
- Navigate to the studio page
- Display data returned from `/videos`
- Display data returned from `/videos/rank`

Anything that would require a new backend endpoint is out of scope for this round.

## Design Direction

The refreshed homepage should feel lighter, more intentional, and more inviting than the current layout while staying within the existing pink-blue identity already present in the project.

### Visual Principles

- Keep the pink and blue palette, but make it feel fresher and less template-like
- Use stronger hierarchy so the eye lands on a featured video first
- Increase the sense of motion and energy through layered backgrounds, badge treatment, and card depth rather than adding fake features
- Make the page feel like a real video homepage, not an admin dashboard with cards

### Tone

- Young
- Bright
- Friendly
- Content-first
- Slightly playful, but still readable and production-appropriate

## Information Architecture

The homepage should be rebuilt into three major sections.

### 1. Refined Header

The site header should keep its current responsibilities but feel more polished.

It should include:

- Brand block linking to `/`
- Primary search form linking to `/search`
- Existing navigation entries
- Current auth-aware user area

This section should not gain any new business behavior. The redesign is visual and structural only.

### 2. Hero Content Stage

The first screen should be the main storytelling surface for the homepage.

#### Left: Featured Video Hero

The large hero card should be populated using real data from the existing public video list. The default featured item should come from the first available item returned by `/videos`.

The hero should show:

- Cover image
- Video title
- Short description if present, otherwise graceful fallback copy
- Author nickname if available from the loaded item shape
- Real stats already available in the loaded video payload, such as views and create time
- Primary CTA to the video detail page
- Secondary CTA to `/search`

This is not a carousel. A static featured slot is safer and fully supported by current data.

#### Right: Hot Ranking Panel

The right column should continue using `/videos/rank`, but present the results as a denser, more eye-catching ranking module.

It should emphasize:

- Ranking order
- Thumbnail and title
- Quick scanning
- Direct click-through to real video pages

The ranking panel remains a real data section, not a decorative sidebar.

### 3. Recommended Video Flow

The lower section should continue using the public video list, but the presentation should feel more editorial and platform-like.

It should include:

- Section header for latest public videos
- A stronger grid rhythm than the current uniform presentation
- More expressive video cards
- Better spacing and clearer metadata hierarchy

The data remains the same. The change is in layout, emphasis, and polish.

## Component-Level Design

### Global CSS

Global tokens in `src/style.css` should be adjusted carefully rather than replaced wholesale.

Expected updates:

- Refine color tokens for fresher surface contrast
- Improve shadows and panel glass treatment
- Introduce a few additional semantic helpers for layered backgrounds and soft accents
- Preserve responsive behavior

### `SiteHeader.vue`

The header should be restyled to support the new homepage tone.

Key changes:

- Stronger brand presentation
- More prominent search area
- Cleaner nav spacing
- Better balance between utility and visual warmth

Functional behavior must remain unchanged.

### `HomeView.vue`

This will be the main implementation surface.

Key changes:

- Split fetched `/videos` data into a featured item and the rest of the list
- Build a new hero composition using the featured video
- Rework ranking layout for better scanability
- Rebuild the lower content area with a stronger editorial rhythm
- Handle empty/loading/error states cleanly using existing API flows

### `VideoCard.vue`

The shared video card component should be visually upgraded so the homepage grid feels more premium and lively.

Possible improvements:

- Stronger hover response
- Better overlay treatment on covers
- Better title and metadata spacing
- More polished compact mode for ranking usage if needed

No new data requirements should be introduced.

## Data Usage Rules

The design must work with the currently loaded payloads and no additional homepage requests.

### `/videos`

Used for:

- Featured hero item
- Main recommendation grid

### `/videos/rank`

Used for:

- Right-side hot ranking module

### No New Requests

The first implementation round must not require:

- New category endpoints
- Personalized recommendation endpoints
- Banner management endpoints
- Creator statistics endpoints
- Tag aggregation endpoints

If a section would need one of those, it should not be built yet.

## Empty, Loading, and Error Handling

The redesigned homepage must stay honest when data is missing.

### Loading

- Show existing loading states with improved visual integration
- Avoid skeleton systems that would require a large new component set for this round

### Error

- Continue using direct, readable error messaging
- Do not hide API failure behind decorative placeholders

### Empty Data

- If `/videos` returns no items, the hero should collapse into a graceful empty state instead of rendering fake featured content
- If `/videos/rank` returns no items, the ranking panel should show an honest empty state

## Responsive Behavior

The redesign should remain mobile-safe and tablet-safe.

### Desktop

- Two-column hero layout
- Multi-column card grid

### Tablet

- Hero stack should collapse vertically
- Ranking remains visible without becoming cramped

### Mobile

- Hero becomes single-column
- CTA area wraps naturally
- Video grid reduces to one column when necessary
- Header already has responsive behavior and should be preserved or improved

## Testing Strategy

This redesign is primarily a presentation update, so validation should focus on correctness and regressions.

### Functional Checks

- Homepage still loads with existing API calls
- Featured hero links to a real `/video/:id` route
- Ranking entries still link to real `/video/:id` routes
- Search CTA still reaches `/search`
- Studio CTA still reaches `/studio`

### Visual Checks

- Hero hierarchy is clear on desktop and mobile
- Cards do not overflow or crop important text badly
- Header remains usable at smaller widths
- Empty and error states still read clearly after restyling

### Build Validation

- `npm run build` in `bilibili_web`

## Out of Scope

The following are explicitly excluded from this homepage round:

- New backend endpoints
- Personalized feed logic
- Fake category tabs without backend support
- Fake live banners
- Fake notification counts
- Cross-page redesign beyond what is required for homepage consistency

## Implementation Summary

The first implementation pass should touch only the minimum set of files needed to produce a visibly improved, video-first homepage:

- `bilibili_web/src/style.css`
- `bilibili_web/src/components/SiteHeader.vue`
- `bilibili_web/src/components/VideoCard.vue`
- `bilibili_web/src/views/HomeView.vue`

The result should feel significantly more beautiful and more like a real video platform homepage while staying fully grounded in existing backend capabilities.
