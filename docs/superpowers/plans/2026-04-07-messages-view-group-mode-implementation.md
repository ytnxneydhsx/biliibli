# Messages View Group Mode Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend the shared `MessagesView` page so the main panel can open either a single chat or a group chat from route query state while preserving the current single-chat flow.

**Architecture:** Keep one `MessagesView` shell and move the mode split into `useMessagesPage`. Normalize active-target state and message-stream keys so the same rendering pipeline can serve both single and group history/realtime payloads. Keep the sidebar single-chat-only for now and let group mode live in the main panel plus route state.

**Tech Stack:** Vue 3 Composition API, TypeScript, Vue Router, Axios, existing IM WebSocket protocol

---

### Task 1: Normalize Messages Page State For Single And Group Targets

**Files:**
- Modify: `bilibili_web/src/features/messages/types.ts`
- Modify: `bilibili_web/src/features/messages/composables/useMessagesPage.ts`

- [ ] **Step 1: Expand frontend message types for group mode**

Add shared target and group payload types in `bilibili_web/src/features/messages/types.ts`:

```ts
export type ActiveTargetType = 'single' | 'group'

export type GroupProfileVO = {
  groupId: string | number
  conversationId?: string
  groupName?: string
  groupAvatar?: string
  memberCount?: number
  status?: number
  isAllMuted?: number
  lastMessage?: string
  lastMessageTime?: string
  lastServerMessageId?: string | number
  lastMessageSeq?: string | number
}

export type GroupConversationUpdatedPayload = {
  groupId?: string | number
  lastServerMessageId?: string | number
}
```

- [ ] **Step 2: Introduce normalized route and stream state**

In `bilibili_web/src/features/messages/composables/useMessagesPage.ts`, replace route parsing and active-target refs with:

```ts
const activeTargetType = ref<ActiveTargetType>('single')
const activePeerUid = ref('')
const activeGroupId = ref('')

const peerUidFromRoute = computed(() => {
  const value = String(route.query.peerUid ?? '').trim()
  return /^\d+$/.test(value) ? value : ''
})

const groupIdFromRoute = computed(() => {
  const value = String(route.query.groupId ?? '').trim()
  return /^\d+$/.test(value) ? value : ''
})
```

Add a shared stream-key helper:

```ts
function buildStreamKey(type: ActiveTargetType, rawId: string) {
  return type === 'group' ? `g:${rawId}` : `s:${rawId}`
}
```

- [ ] **Step 3: Generalize in-memory message storage**

Rename message state from peer-only storage to stream-key storage:

```ts
const messagesByStream = ref<Record<string, MessageItem[]>>({})
```

Update computed accessors to read from the normalized stream:

```ts
const activeStreamKey = computed(() => {
  if (activeTargetType.value === 'group' && activeGroupId.value) {
    return buildStreamKey('group', activeGroupId.value)
  }
  if (activeTargetType.value === 'single' && activePeerUid.value) {
    return buildStreamKey('single', activePeerUid.value)
  }
  return ''
})

const activeMessages = computed(() => {
  if (!activeStreamKey.value) return []
  return messagesByStream.value[activeStreamKey.value] || []
})
```

- [ ] **Step 4: Update merge helpers to operate on stream keys**

Replace `mergeMessages(peerUid, incoming)` with:

```ts
function mergeMessages(streamKey: string, incoming: MessageItem[]) {
  const current = messagesByStream.value[streamKey] || []
  const merged = new Map<string, MessageItem>()

  for (const item of current) {
    merged.set(item.dedupeKey, item)
  }
  for (const item of incoming) {
    const existing = merged.get(item.dedupeKey)
    merged.set(item.dedupeKey, existing ? { ...existing, ...item } : item)
  }

  const next = Array.from(merged.values()).sort((left, right) => {
    if (left.epoch === right.epoch) {
      return left.dedupeKey.localeCompare(right.dedupeKey)
    }
    return left.epoch - right.epoch
  })

  messagesByStream.value = {
    ...messagesByStream.value,
    [streamKey]: next,
  }
}
```

- [ ] **Step 5: Commit normalized state groundwork**

```bash
git add bilibili_web/src/features/messages/types.ts \
        bilibili_web/src/features/messages/composables/useMessagesPage.ts \
        docs/superpowers/plans/2026-04-07-messages-view-group-mode-implementation.md
git commit -m "refactor(web-messages): normalize target state for group mode"
```

### Task 2: Add Group History And Group Profile Loading

**Files:**
- Modify: `bilibili_web/src/features/messages/composables/useMessagesPage.ts`
- Modify: `bilibili_web/src/features/messages/types.ts`

- [ ] **Step 1: Add group-profile cache and group activation flow**

In `useMessagesPage.ts`, add:

```ts
const groupProfiles = ref<Record<string, GroupProfileVO>>({})
const loadingGroup = ref(false)
```

Create a group activation function:

```ts
async function activateGroup(groupId: string) {
  activeTargetType.value = 'group'
  activeGroupId.value = groupId
  activePeerUid.value = ''
  await Promise.all([
    loadGroupProfile(groupId),
    ensureGroupHistoryLoaded(groupId),
  ])
  await nextTick()
  scrollStreamToBottom()
}
```

- [ ] **Step 2: Load group profile from backend**

Add a loader using `GET /me/im/groups/{groupId}`:

```ts
async function loadGroupProfile(groupId: string) {
  if (!groupId || groupProfiles.value[groupId]) {
    return
  }
  loadingGroup.value = true
  try {
    const profile = await api.get<GroupProfileVO>(`/me/im/groups/${groupId}`)
    groupProfiles.value = {
      ...groupProfiles.value,
      [groupId]: profile,
    }
  } catch {
    // keep page usable even if group profile fetch fails
  } finally {
    loadingGroup.value = false
  }
}
```

- [ ] **Step 3: Add group-history loading through the new backend API**

Add helpers:

```ts
async function ensureGroupHistoryLoaded(groupId: string) {
  const streamKey = buildStreamKey('group', groupId)
  if (messagesByStream.value[streamKey]?.length) {
    return
  }
  await loadGroupHistoryPage(groupId, false)
}

async function loadGroupHistoryPage(groupId: string, appendOlder: boolean) {
  const profile = groupProfiles.value[groupId]
  if (appendOlder && (!profile?.lastServerMessageId || !activeMessages.value.length)) {
    // rely on message array cursor instead of sidebar conversation metadata
  }

  loadingHistory.value = true
  try {
    const current = messagesByStream.value[buildStreamKey('group', groupId)] || []
    const beforeServerMessageId =
      appendOlder && current.length ? current[0].serverMessageId || undefined : undefined
    const history = await api.get<MessageHistoryVO>(`/me/im/groups/${groupId}/messages/history`, {
      beforeServerMessageId,
    })
    const mapped = (history.records || []).map((record) => toGroupMessageItem(record, groupId))
    mergeMessages(buildStreamKey('group', groupId), mapped)
  } catch (error) {
    pushEvent('error', (error as Error).message || '群历史消息加载失败')
  } finally {
    loadingHistory.value = false
  }
}
```

- [ ] **Step 4: Wire route watcher to choose single or group mode**

Replace the peer-only route watcher with:

```ts
watch(
  [groupIdFromRoute, peerUidFromRoute],
  async ([groupId, peerUid]) => {
    if (groupId) {
      await activateGroup(groupId)
      return
    }
    if (peerUid) {
      await activateConversation(peerUid)
      return
    }
    activePeerUid.value = ''
    activeGroupId.value = ''
  },
  { immediate: true },
)
```

- [ ] **Step 5: Commit group loading support**

```bash
git add bilibili_web/src/features/messages/types.ts \
        bilibili_web/src/features/messages/composables/useMessagesPage.ts
git commit -m "feat(web-messages): load group history in shared message view"
```

### Task 3: Handle Group WebSocket Events And Shared Header Rendering

**Files:**
- Modify: `bilibili_web/src/features/messages/composables/useMessagesPage.ts`
- Modify: `bilibili_web/src/views/MessagesView.vue`

- [ ] **Step 1: Route `message_received` by conversation id prefix**

In `handleSocketMessage`, add:

```ts
if (type === 'group_conversation_updated') {
  handleGroupConversationUpdated(packet.data as GroupConversationUpdatedPayload | undefined)
  return
}
```

Split `handleReceived`:

```ts
if (String(data.conversationId || '').startsWith('g_')) {
  await handleReceivedGroupMessage(data)
  return
}
await handleReceivedSingleMessage(data)
```

- [ ] **Step 2: Merge realtime group messages into the active group stream**

Add:

```ts
async function handleReceivedGroupMessage(data: MessagePushPayload) {
  const groupId = String(data.receiverId || '')
  if (!groupId) {
    return
  }
  await loadGroupProfile(groupId)
  mergeMessages(buildStreamKey('group', groupId), [toGroupRealtimeMessageItem(data, groupId)])
  if (activeTargetType.value === 'group' && activeGroupId.value === groupId) {
    void nextTick().then(scrollStreamToBottom)
  }
}
```

Keep `group_conversation_updated` lightweight:

```ts
function handleGroupConversationUpdated(data?: GroupConversationUpdatedPayload) {
  if (!data?.groupId) {
    return
  }
  pushEvent('group_conversation_updated', JSON.stringify(data, null, 2))
}
```

- [ ] **Step 3: Switch the main-panel title, subtitle, and empty state by active mode**

In `useMessagesPage.ts`, add:

```ts
const activeGroupProfile = computed(() => {
  if (!activeGroupId.value) return null
  return groupProfiles.value[activeGroupId.value] || null
})
```

Update title/subtitle:

```ts
const activeConversationTitle = computed(() => {
  if (activeTargetType.value === 'group' && activeGroupId.value) {
    return activeGroupProfile.value?.groupName || `群聊 ${activeGroupId.value}`
  }
  ...
})

const activeConversationSubtitle = computed(() => {
  if (activeTargetType.value === 'group' && activeGroupId.value) {
    if (loadingGroup.value && !activeGroupProfile.value) return '正在补全群资料。'
    return activeGroupProfile.value?.memberCount
      ? `${activeGroupProfile.value.memberCount} 位成员`
      : '支持最近消息缓存、实时收发和群会话通知。'
  }
  ...
})
```

- [ ] **Step 4: Update the view and composer bindings**

In `MessagesView.vue`, bind the header fallback and empty state to the new mode fields, and make composer read-only in group mode until send-path support lands:

```vue
<MessagesComposer
  :active-peer-uid="activeTargetType === 'single' ? activePeerUid : ''"
  ...
/>
```

Also update the empty-state copy:

```vue
<div v-if="!activePeerUid && !activeGroupId" class="empty-state">
  先从私信入口进入单聊，或者通过群入口带上 groupId 打开群聊模式。
</div>
```

- [ ] **Step 5: Commit realtime group-mode UI integration**

```bash
git add bilibili_web/src/features/messages/composables/useMessagesPage.ts \
        bilibili_web/src/views/MessagesView.vue
git commit -m "feat(web-messages): handle realtime group mode in shared view"
```

### Task 4: Verify Build And Publish

**Files:**
- Verify only

- [ ] **Step 1: Run the frontend build**

Run:

```bash
cd /home/huangnv/biliibli/bilibili_web
npm run build
```

Expected:

```text
vue-tsc -b && vite build
✓ built in ...
```

- [ ] **Step 2: Inspect git diff**

Run:

```bash
cd /home/huangnv/biliibli
git status --short
```

Expected:

```text
M bilibili_web/src/features/messages/...
M bilibili_web/src/views/MessagesView.vue
```

- [ ] **Step 3: Commit verification fixes if needed**

```bash
git add bilibili_web/src/features/messages/types.ts \
        bilibili_web/src/features/messages/composables/useMessagesPage.ts \
        bilibili_web/src/views/MessagesView.vue
git commit -m "chore(web-messages): verify shared group mode build"
```

- [ ] **Step 4: Push to remote**

```bash
cd /home/huangnv/biliibli
git push origin master
```
