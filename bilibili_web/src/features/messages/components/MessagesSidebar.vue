<script setup lang="ts">
import { computed } from 'vue'
import type { ActiveTargetType, ConnectionState, ConversationItem, GroupConversationWindowVO } from '../types'
import type { UserProfileVO } from '../../../types'

const props = defineProps<{
  currentProfile: UserProfileVO | null | undefined
  currentUid: string
  currentUsername: string
  connectionState: ConnectionState
  connectionLabel: string
  activeTab: ActiveTargetType
  singleConversations: ConversationItem[]
  groupConversations: GroupConversationWindowVO[]
  activePeerUid: string
  activeGroupId: string
  resolvePeerName: (peerUid: string) => string
  resolvePeerAvatar: (peerUid: string) => string
  resolveGroupName: (groupId: string) => string
  resolveGroupAvatar: (groupId: string) => string
}>()

const emit = defineEmits<{
  reconnect: []
  disconnect: []
  switchTab: [tab: ActiveTargetType]
  openSingle: [peerUid: string]
  openGroup: [groupId: string]
}>()

const isSingleTab = computed(() => props.activeTab === 'single')
</script>

<template>
  <aside class="messages-sidebar">
    <nav class="message-center-nav">
      <div class="nav-title">
        <span class="nav-arrow">▸</span>
        <strong>消息中心</strong>
      </div>

      <button
        class="nav-item"
        :class="{ active: activeTab === 'single' }"
        type="button"
        @click="emit('switchTab', 'single')"
      >
        <span>私人聊天</span>
        <b>{{ singleConversations.length }}</b>
      </button>
      <button
        class="nav-item"
        :class="{ active: activeTab === 'group' }"
        type="button"
        @click="emit('switchTab', 'group')"
      >
        <span>群聊天</span>
        <b>{{ groupConversations.length }}</b>
      </button>

      <div class="connection-block">
        <span class="connection-dot" :class="`state-${connectionState}`"></span>
        <span>{{ connectionLabel }}</span>
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
          <span>UID {{ currentUid }}</span>
        </div>
        <div class="sidebar-actions">
          <button type="button" @click="emit('reconnect')">重连</button>
          <button type="button" @click="emit('disconnect')">断开</button>
        </div>
      </section>
    </nav>

    <section class="recent-pane">
      <header class="recent-header">
        <span>最近消息</span>
      </header>

      <div v-if="isSingleTab ? singleConversations.length : groupConversations.length" class="conversation-list">
      <button
        v-if="isSingleTab"
        v-for="conversation in singleConversations"
        :key="conversation.peerUid"
        class="conversation-item"
        :class="{ active: activePeerUid === conversation.peerUid }"
        type="button"
        @click="emit('openSingle', conversation.peerUid)"
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
            <span>{{ conversation.lastMessageTime || '' }}</span>
          </div>
          <p>{{ conversation.lastMessage || '还没有消息' }}</p>
        </div>
        <span v-if="conversation.unreadCount > 0" class="conversation-badge">{{ conversation.unreadCount }}</span>
      </button>

      <button
        v-else
        v-for="conversation in groupConversations"
        :key="String(conversation.groupId || '')"
        class="conversation-item"
        :class="{ active: activeGroupId === String(conversation.groupId || '') }"
        type="button"
        @click="emit('openGroup', String(conversation.groupId || ''))"
      >
        <img
          v-if="resolveGroupAvatar(String(conversation.groupId || ''))"
          class="conversation-avatar"
          :src="resolveGroupAvatar(String(conversation.groupId || ''))"
          :alt="resolveGroupName(String(conversation.groupId || ''))"
        />
        <div v-else class="conversation-avatar fallback">
          {{ resolveGroupName(String(conversation.groupId || '')).slice(0, 1) }}
        </div>
        <div class="conversation-copy">
          <div class="conversation-topline">
            <strong>{{ resolveGroupName(String(conversation.groupId || '')) }}</strong>
            <span>{{ conversation.lastMessageTime || '' }}</span>
          </div>
          <p>{{ conversation.lastMessage || '还没有消息' }}</p>
        </div>
        <span v-if="Number(conversation.unreadCount || 0) > 0" class="conversation-badge">{{ conversation.unreadCount }}</span>
      </button>
      </div>
      <div v-else class="empty-state">
        <template v-if="isSingleTab">
          从用户主页点击“私信”进入一个私人会话。
        </template>
        <template v-else>
          当前还没有群聊窗口。
        </template>
      </div>
    </section>
  </aside>
</template>

<style scoped>
.messages-sidebar {
  display: grid;
  grid-template-columns: 140px 260px;
  min-height: calc(100vh - 32px);
  border-right: 1px solid var(--bili-line, #e3e5e7);
  background: rgba(18, 29, 43, 0.82);
  color: var(--bili-text, #f7fbff);
}

.message-center-nav {
  padding: 20px 18px;
  background: rgba(16, 27, 41, 0.94);
  border-right: 1px solid var(--bili-line, #e3e5e7);
}

.nav-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 26px;
  color: var(--bili-text, #f7fbff);
  font-size: 15px;
}

.nav-arrow {
  color: var(--bili-blue, #32c5ff);
  font-size: 15px;
}

.nav-item {
  width: 100%;
  min-height: 38px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--bili-muted, #8ea1b8);
  font-weight: 700;
  text-align: left;
}

.nav-item::before {
  content: "";
  width: 4px;
  height: 4px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: currentColor;
}

.nav-item span {
  flex: 1;
}

.nav-item b {
  color: var(--bili-pink, #fb7299);
  font-size: 12px;
}

.nav-item.active {
  color: var(--bili-blue, #00aeec);
}

.connection-block {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 30px 0 12px;
  color: var(--bili-muted, #61666d);
  font-size: 13px;
}

.connection-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(142, 161, 184, 0.5);
}

.connection-dot.state-live {
  background: var(--bili-blue, #00aeec);
}

.connection-dot.state-error {
  background: var(--bili-pink, #fb7299);
}

.identity-card {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  padding: 10px;
  border-radius: 8px;
  border: 1px solid var(--bili-line, rgba(165, 188, 218, 0.12));
  background: linear-gradient(135deg, rgba(251, 114, 153, 0.1), rgba(50, 197, 255, 0.1));
}

.identity-avatar,
.conversation-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  object-fit: cover;
}

.identity-fallback,
.conversation-avatar.fallback {
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, var(--bili-pink, #fb7299) 0%, #ff9fbc 100%);
  color: #fff;
  font-weight: 800;
}

.identity-copy {
  display: grid;
  gap: 2px;
  min-width: 0;
  font-size: 13px;
}

.identity-copy span {
  overflow: hidden;
  color: var(--bili-subtle, #9499a0);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sidebar-actions {
  grid-column: 1 / -1;
  display: flex;
  gap: 8px;
}

.sidebar-actions button {
  height: 28px;
  padding: 0 12px;
  border: 1px solid var(--line-strong, rgba(187, 208, 235, 0.2));
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.07);
  color: var(--bili-text, #f7fbff);
  font-size: 12px;
}

.recent-pane {
  min-width: 0;
  background: rgba(18, 29, 43, 0.82);
}

.recent-header {
  height: 44px;
  display: flex;
  align-items: center;
  padding: 0 18px;
  border-bottom: 1px solid var(--bili-line-soft, #f1f2f3);
  color: var(--bili-muted, #8ea1b8);
  font-size: 13px;
}

.conversation-list {
  display: grid;
}

.conversation-item {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  width: 100%;
  min-height: 76px;
  padding: 10px 14px;
  border: 0;
  border-bottom: 1px solid var(--bili-line-soft, #f1f2f3);
  border-radius: 0;
  background: transparent;
  color: var(--bili-text, #f7fbff);
  text-align: left;
  transition: background 0.16s ease;
}

.conversation-item:hover {
  background: rgba(255, 255, 255, 0.06);
}

.conversation-item.active {
  background: linear-gradient(135deg, rgba(50, 197, 255, 0.16), rgba(251, 114, 153, 0.1));
}

.conversation-avatar {
  width: 42px;
  height: 42px;
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

.conversation-topline strong {
  overflow: hidden;
  color: var(--bili-text, #f7fbff);
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conversation-topline span {
  flex: 0 0 auto;
  color: var(--bili-subtle, #9499a0);
  font-size: 12px;
}

.conversation-copy p {
  margin: 6px 0 0;
  color: var(--bili-subtle, #9499a0);
  font-size: 13px;
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
  background: var(--bili-pink, #fb7299);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.empty-state {
  margin: 14px;
  padding: 16px;
  border: 1px dashed var(--bili-line, #e3e5e7);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.05);
  color: var(--bili-subtle, #9499a0);
  font-size: 13px;
}

@media (max-width: 820px) {
  .messages-sidebar {
    grid-template-columns: 1fr;
    min-height: auto;
    border-right: 0;
  }

  .message-center-nav {
    border-right: 0;
    border-bottom: 1px solid var(--bili-line, #e3e5e7);
  }

  .identity-card {
    max-width: 280px;
  }
}
</style>
