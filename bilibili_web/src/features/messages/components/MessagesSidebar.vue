<script setup lang="ts">
import type { ConversationItem, ConnectionState } from '../types'
import type { UserProfileVO } from '../../../types'

defineProps<{
  currentProfile: UserProfileVO | null | undefined
  currentUid: string
  currentUsername: string
  connectionState: ConnectionState
  connectionLabel: string
  conversations: ConversationItem[]
  activePeerUid: string
  resolvePeerName: (peerUid: string) => string
  resolvePeerAvatar: (peerUid: string) => string
}>()

const emit = defineEmits<{
  reconnect: []
  disconnect: []
  open: [peerUid: string]
}>()
</script>

<template>
  <aside class="messages-sidebar panel">
    <div class="section-title">
      <div>
        <h2>私信</h2>
        <p class="muted">复用主站登录态与资料状态。</p>
      </div>
      <span class="pill" :class="`state-${connectionState}`">{{ connectionLabel }}</span>
    </div>

    <section class="identity-card">
      <img
        v-if="currentProfile?.avatar"
        class="identity-avatar"
        :src="currentProfile.avatar"
        :alt="currentProfile.nickname"
      />
      <div v-else class="identity-avatar identity-fallback">
        {{ (currentProfile?.nickname || currentUsername || '我').slice(0, 1) }}
      </div>
      <div class="identity-copy">
        <strong>{{ currentProfile?.nickname || currentUsername || '当前用户' }}</strong>
        <span class="muted">UID {{ currentUid }}</span>
      </div>
      <div class="sidebar-actions">
        <button class="secondary-button" type="button" @click="emit('reconnect')">重连</button>
        <button class="secondary-button" type="button" @click="emit('disconnect')">断开</button>
      </div>
    </section>

    <div v-if="conversations.length" class="conversation-list">
      <button
        v-for="conversation in conversations"
        :key="conversation.peerUid"
        class="conversation-item"
        :class="{ active: activePeerUid === conversation.peerUid }"
        type="button"
        @click="emit('open', conversation.peerUid)"
      >
        <img
          v-if="resolvePeerAvatar(conversation.peerUid)"
          class="conversation-avatar"
          :src="resolvePeerAvatar(conversation.peerUid)"
          :alt="resolvePeerName(conversation.peerUid)"
        />
        <div v-else class="conversation-avatar fallback">
          {{ resolvePeerName(conversation.peerUid).slice(0, 1) }}
        </div>
        <div class="conversation-copy">
          <div class="conversation-topline">
            <strong>{{ resolvePeerName(conversation.peerUid) }}</strong>
            <span class="muted">{{ conversation.lastMessageTime || '' }}</span>
          </div>
          <p>{{ conversation.lastMessage || '还没有消息' }}</p>
        </div>
        <span v-if="conversation.unreadCount > 0" class="conversation-badge">{{ conversation.unreadCount }}</span>
      </button>
    </div>
    <div v-else class="empty-state">
      从用户主页点击“私信”进入一个会话，或者等待实时窗口推送出现在这里。
    </div>
  </aside>
</template>

<style scoped>
.messages-sidebar {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 22px;
}

.identity-card {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
  padding: 16px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(251, 114, 153, 0.08), rgba(0, 161, 214, 0.1));
}

.identity-avatar,
.conversation-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  object-fit: cover;
}

.identity-fallback,
.conversation-avatar.fallback {
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, var(--pink) 0%, #ff9ab6 100%);
  color: #fff;
  font-weight: 800;
}

.identity-copy {
  display: grid;
  gap: 4px;
}

.sidebar-actions {
  grid-column: 1 / -1;
  display: flex;
  gap: 10px;
}

.conversation-list {
  display: grid;
  gap: 10px;
}

.conversation-item {
  display: grid;
  grid-template-columns: 46px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 12px;
  border-radius: 18px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.76);
  transition: transform 0.18s ease, border-color 0.18s ease, background 0.18s ease;
}

.conversation-item:hover {
  transform: translateY(-1px);
}

.conversation-item.active {
  border-color: rgba(0, 161, 214, 0.28);
  background: rgba(223, 246, 255, 0.78);
}

.conversation-avatar {
  width: 46px;
  height: 46px;
}

.conversation-copy {
  min-width: 0;
}

.conversation-topline {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.conversation-copy p {
  margin: 6px 0 0;
  color: var(--muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conversation-badge {
  min-width: 22px;
  height: 22px;
  display: inline-grid;
  place-items: center;
  padding: 0 6px;
  border-radius: 999px;
  background: var(--pink);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.state-idle,
.state-connecting {
  color: var(--muted);
}

.state-live {
  color: var(--blue);
  background: var(--blue-soft);
}

.state-error {
  color: var(--danger);
  background: rgba(255, 92, 124, 0.1);
}

@media (max-width: 720px) {
  .messages-sidebar {
    padding: 16px;
  }
}
</style>
