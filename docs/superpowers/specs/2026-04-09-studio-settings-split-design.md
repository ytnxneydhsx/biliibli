# Studio Settings Split Design

**Date:** 2026-04-09

**Goal:** Split the current combined `/studio` page in `bilibili_web` into two clear authenticated pages: `/studio` for video publishing only, and `/settings` for personal profile editing only.

## Product Intent

The current experience mixes two unrelated jobs on one page:

- publish video content
- edit personal profile information

That makes the page harder to understand, especially for new users. The split should make the product feel clearer and more intentional:

- `创作中心` should mean "publish content"
- `资料设置` should mean "edit my personal information"

## Current State

### Existing Frontend Structure

- Route `/studio` points to `StudioView.vue`
- `StudioView.vue` currently contains both:
  - profile editing and avatar update
  - cover upload, video file upload, and publish flow
- Header navigation shows both `创作中心` and `资料设置`, but both links currently point to `/studio`

### Existing Backend Capabilities

No new backend endpoints are required for this split.

Current frontend behavior already uses real working APIs:

- `PUT /me/profile` for profile updates
- `POST /me/uploads/avatar` for avatar uploads
- `POST /me/uploads/video-cover` for cover uploads
- `POST /me/videos/uploads/init-session` for upload session initialization
- `POST /me/videos/uploads/{uploadId}/parts/sign` for part signing
- `POST /me/videos/uploads/{uploadId}/complete` for final publish
- `DELETE /me/videos/uploads/{uploadId}` for upload cleanup

## Approved Direction

Use a clear two-page split.

### `/studio`

This page should keep only creator and publishing responsibilities:

- video title
- video description
- video cover upload
- video file selection
- duration probing
- multipart upload progress
- final publish action

This page should no longer contain avatar, nickname, or signature editing.

### `/settings`

This page should be a new authenticated page dedicated to personal information management:

- current avatar preview
- avatar upload
- nickname update
- signature update
- save result feedback

This page should not contain video publishing UI.

## Navigation Changes

### Header

Header navigation should become semantically correct:

- `创作中心` -> `/studio`
- `资料设置` -> `/settings`

Both pages should keep the same authentication requirement behavior as the current `/studio` page.

### Other In-App Entrances

Any UI that conceptually means "edit my profile" should go to `/settings`.

Any UI that conceptually means "start publishing" should go to `/studio`.

For this round, the most important requirement is the top-level header split. Additional entry cleanup should only be done where already obvious and low-risk.

## View Responsibilities

### `StudioView.vue`

After the split, `StudioView.vue` should become smaller and more focused.

It should keep:

- upload-related reactive state
- cover upload state
- video file handling
- multipart upload flow
- publish feedback

It should remove:

- profile form state
- profile save handler
- avatar upload state and handler
- profile synchronization helpers

### New `SettingsView.vue`

Create a new view that owns the extracted profile-editing behavior.

It should include:

- profile form state
- profile update request
- avatar upload flow
- profile refresh after successful avatar upload
- user-friendly page copy and layout

It may reuse the same API calls and auth state already used by the current `StudioView`.

## Routing Rules

Add a new authenticated route:

- path: `/settings`
- name: `settings`
- component: `SettingsView.vue`
- `meta.requiresAuth = true`

Keep the current `/studio` route authenticated as-is.

Unauthenticated users visiting either page should continue to be redirected through the existing router guard to `/auth`.

## UI Expectations

The split should improve clarity, not invent new product behavior.

### Studio Page Tone

The page should feel like a lightweight creator workspace.

Good emphasis:

- upload intent
- progress
- completion feedback

Avoid:

- account settings copy
- personal profile framing

### Settings Page Tone

The page should feel like a calm personal profile editor.

Good emphasis:

- avatar and identity
- nickname and signature
- simple save flow

Avoid:

- upload studio language
- creator publishing jargon

## Technical Constraints

- No backend API changes
- No auth model changes
- No new permissions model
- No changes to upload protocol
- No changes to profile update payload shape

This is a frontend structure and UX split only.

## Validation Requirements

After implementation, verify:

- `/studio` is still reachable after login
- `/studio` only shows upload-related UI
- `/settings` is reachable after login
- `/settings` shows avatar + nickname + signature editing
- header links go to different routes
- unauthenticated visits to `/studio` and `/settings` redirect to `/auth`
- profile save still works
- avatar upload still works
- video publish flow still builds and renders

## Files Expected To Change

- `bilibili_web/src/router.ts`
- `bilibili_web/src/components/SiteHeader.vue`
- `bilibili_web/src/views/StudioView.vue`
- `bilibili_web/src/views/SettingsView.vue`

Tests will likely also be added or updated for routing and page separation.
