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
  <aside class="messages-sidebar panel">
    <div class="section-title">
      <div>
        <h2>消息</h2>
        <p class="muted">把私人聊天和群聊天分开来看，会更清楚一点。</p>
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

    <div class="sidebar-tabs">
      <button
        class="sidebar-tab"
        :class="{ active: activeTab === 'single' }"
        type="button"
        @click="emit('switchTab', 'single')"
      >
        私人聊天
        <span class="tab-count">{{ singleConversations.length }}</span>
      </button>
      <button
        class="sidebar-tab"
        :class="{ active: activeTab === 'group' }"
        type="button"
        @click="emit('switchTab', 'group')"
      >
        群聊天
        <span class="tab-count">{{ groupConversations.length }}</span>
      </button>
    </div>

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
            <span class="muted">{{ conversation.lastMessageTime || '' }}</span>
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
            <span class="muted">{{ conversation.lastMessageTime || '' }}</span>
          </div>
          <p>{{ conversation.lastMessage || '还没有消息' }}</p>
        </div>
        <span v-if="Number(conversation.unreadCount || 0) > 0" class="conversation-badge">{{ conversation.unreadCount }}</span>
      </button>
    </div>
    <div v-else class="empty-state">
      <template v-if="isSingleTab">
        从用户主页点击“私信”进入一个私人会话，左侧就会出现在这里。
      </template>
      <template v-else>
        当前还没有群聊窗口，后面有群聊消息时会集中显示在这里。
      </template>
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

.sidebar-tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.sidebar-tab {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 44px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.76);
  color: var(--muted);
  font-weight: 700;
  transition: border-color 0.18s ease, background 0.18s ease, color 0.18s ease;
}

.sidebar-tab.active {
  border-color: rgba(0, 161, 214, 0.28);
  background: rgba(223, 246, 255, 0.78);
  color: var(--text);
}

.tab-count {
  min-width: 24px;
  height: 24px;
  display: inline-grid;
  place-items: center;
  padding: 0 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.95);
  color: var(--blue);
  font-size: 12px;
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
