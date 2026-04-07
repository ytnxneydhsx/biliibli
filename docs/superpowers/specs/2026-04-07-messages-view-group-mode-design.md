# Messages View Group Mode Design

## Goal

Extend the existing shared `MessagesView` page so the main panel can switch between:

- single chat mode
- group chat mode

The page remains a single shared entry, but the active target is determined by route params:

- `peerUid` for single chat
- `groupId` for group chat

This first iteration focuses on enabling group chat in the main panel without rewriting the sidebar into a mixed single/group window list yet.

## Scope

In scope:

- route-level support for `groupId`
- composable support for active target type discrimination
- group history loading
- group realtime message receiving
- group conversation update notification handling
- group read advancement trigger on recent-history load
- main panel title/subtitle rendering for group mode
- shared message stream rendering for both modes

Out of scope:

- merging group windows into the existing sidebar list
- group member list UI
- group-specific composer permissions or mute-state UX
- complete pagination redesign for group windows
- creating a separate group chat page

## Current State

The frontend is currently single-chat only:

- route query uses `peerUid`
- sidebar data model is keyed by peer user id
- history reads `/me/im/messages/history`
- read clearing calls `/me/im/conversations/read`
- websocket handling understands:
  - `message_received`
  - `conversation_updated`

The backend now additionally supports:

- group history: `GET /me/im/groups/{groupId}/messages/history`
- group recent-message cache
- group realtime push via `message_received`
- group conversation change websocket event:
  - `group_conversation_updated`

## Recommended Approach

Keep one shared `MessagesView`, but introduce a target discriminator inside the composable.

Recommended active target model:

- `type = 'single' | 'group'`
- `peerUid` only exists for single mode
- `groupId` only exists for group mode

Why this approach:

- it preserves the current page shell and visual structure
- it minimizes template churn
- it allows message rendering and websocket handling to be generalized
- it avoids prematurely redesigning the sidebar

Alternative approaches considered:

1. Separate group page
- simpler local implementation
- but duplicates message-page structure and diverges UX

2. Fully merge sidebar into mixed single/group windows now
- more complete
- but much larger refactor and riskier for the existing single-chat flow

## Route Design

The messages route will accept either:

- `?peerUid=...`
- `?groupId=...`

Resolution rules:

- if `groupId` exists, open group mode
- else if `peerUid` exists, open single mode
- else fall back to current default behavior

Only one mode should be active at a time.

## Frontend State Model

The composable should maintain a normalized active target:

- `activeTargetType`
- `activePeerUid`
- `activeGroupId`

Conversation list data remains single-chat only for now.

Message stream state should be keyed by a normalized stream key:

- single: `s:{peerUid}`
- group: `g:{groupId}`

This avoids collisions and lets the same message-stream logic be reused.

## Data Loading

### Single Mode

Keep current behavior:

- load single windows
- load single history through `/me/im/messages/history`
- mark unread cleared through `/me/im/conversations/read`

### Group Mode

When activating a group:

1. ensure group profile/basic info is loaded
2. load recent history from:
   - `GET /me/im/groups/{groupId}/messages/history`
3. rely on backend recent-history query to advance `lastReadSeq`
4. render the shared message stream with group messages

Older history continues to use the same group history endpoint with `beforeServerMessageId`.

## WebSocket Handling

### `message_received`

Reuse this event for both single and group messages.

Dispatch rule:

- if `conversationId` starts with `g_`, treat it as group message
- otherwise treat it as single message

For group messages:

- merge the message into the group stream
- if the active mode is this group, keep unread visually at zero in the local view
- do not attempt to fake group window counts in the sidebar yet

### `group_conversation_updated`

Handle this as a lightweight notification:

- if the active mode is the matching group, optionally refresh group history if needed later
- for this first iteration, store the event in logs and leave room for later group-window-list integration

This preserves compatibility with the backend without forcing a sidebar refactor in the same pass.

## UI Behavior

Main panel header switches by mode.

Single mode:

- current peer title/subtitle behavior stays

Group mode:

- title shows group name
- subtitle shows a simple group description such as member count or fallback text

Composer remains shared.

For this iteration we assume:

- sending to group will reuse the current websocket send flow once the frontend payload is adjusted later
- if send-path adjustment is not completed in the same pass, group mode can remain read-first

## Error Handling

- invalid or missing `groupId` should keep the page usable and show a neutral empty-state message
- failed group history load should log an error event without breaking single mode
- websocket group events with incomplete payload should be ignored safely

## Testing and Verification

Manual verification targets:

1. existing single-chat flow still works unchanged
2. opening a route with `groupId` loads group history
3. group websocket `message_received` appends to the active group stream
4. group websocket `group_conversation_updated` is parsed without breaking the page
5. switching between single and group routes does not leak old message state

## Implementation Slices

Recommended implementation order:

1. normalize route and active-target state
2. add group history querying to the composable
3. generalize message-stream storage to support single and group keys
4. add websocket dispatch for group messages and group conversation updates
5. adapt main-panel header and empty-state for group mode
6. verify existing single-chat behavior still holds

