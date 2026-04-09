<script setup lang="ts">
import MessagesDebugPanel from '../features/messages/components/MessagesDebugPanel.vue'
import MessageBubble from '../features/messages/components/MessageBubble.vue'
import MessagesComposer from '../features/messages/components/MessagesComposer.vue'
import MessagesSidebar from '../features/messages/components/MessagesSidebar.vue'
import { useMessagesPage } from '../features/messages/composables/useMessagesPage'

const {
  activeConversationSubtitle,
  activeConversationTitle,
  activeGroupId,
  activeGroupProfile,
  activeHasMoreHistory,
  activeMessages,
  activePeerProfile,
  activePeerUid,
  activeTargetType,
  canSend,
  connectionLabel,
  connectionState,
  connectSocket,
  currentProfile,
  currentUid,
  currentUsername,
  disconnectSocket,
  draftImages,
  eventLogs,
  hasFailedImages,
  hasUploadingImages,
  loadOlderMessages,
  loadingHistory,
  messageDraft,
  messageStream,
  openConversation,
  openGroupConversation,
  removeDraftImage,
  resolveGroupAvatar,
  resolveGroupName,
  resolveMessagePeerName,
  resolvePeerAvatar,
  resolvePeerName,
  sendMessage,
  setSidebarTab,
  sidebarTab,
  setMessageDraft,
  sortedGroupConversations,
  sortedSingleConversations,
  uploadError,
  uploadImages,
} = useMessagesPage()

void messageStream
</script>

<template>
  <section class="messages-page">
    <MessagesSidebar
      :current-profile="currentProfile"
      :current-uid="currentUid"
      :current-username="currentUsername"
      :connection-state="connectionState"
      :connection-label="connectionLabel"
      :active-tab="sidebarTab"
      :single-conversations="sortedSingleConversations"
      :group-conversations="sortedGroupConversations"
      :active-peer-uid="activePeerUid"
      :active-group-id="activeGroupId"
      :resolve-group-name="resolveGroupName"
      :resolve-group-avatar="resolveGroupAvatar"
      :resolve-peer-name="resolvePeerName"
      :resolve-peer-avatar="resolvePeerAvatar"
      @reconnect="connectSocket"
      @disconnect="disconnectSocket"
      @switch-tab="setSidebarTab"
      @open-single="openConversation"
      @open-group="openGroupConversation"
    />

    <section class="messages-main panel">
      <header class="chat-header">
        <div class="chat-peer">
          <img
            v-if="activeTargetType === 'group' ? activeGroupProfile?.groupAvatar : activePeerProfile?.avatar"
            class="chat-avatar"
            :src="activeTargetType === 'group' ? activeGroupProfile?.groupAvatar : activePeerProfile?.avatar"
            :alt="activeConversationTitle"
          />
          <div v-else class="chat-avatar fallback">
            {{
              activeTargetType === 'group'
                ? (activeConversationTitle || '群').slice(0, 1)
                : activePeerUid
                  ? resolvePeerName(activePeerUid).slice(0, 1)
                  : '私'
            }}
          </div>
          <div>
            <h2>{{ activeConversationTitle }}</h2>
            <p class="muted">{{ activeConversationSubtitle }}</p>
          </div>
        </div>
      </header>

      <div ref="messageStream" class="message-stream">
        <div v-if="(activePeerUid || activeGroupId) && activeHasMoreHistory" class="history-actions">
          <button class="secondary-button" type="button" :disabled="loadingHistory" @click="loadOlderMessages">
            {{ loadingHistory ? '加载中…' : '加载更早消息' }}
          </button>
        </div>

        <div v-if="!activePeerUid && !activeGroupId" class="empty-state">
          <template v-if="sidebarTab === 'group'">
            群聊天现在单独收进一个分栏里了。等有群聊窗口时，会集中显示在左侧这一栏。
          </template>
          <template v-else>
            私人聊天会单独显示在左侧。先去任意用户主页点“私信”，这里就会出现对应会话。
          </template>
        </div>
        <div v-else-if="!activeMessages.length && !loadingHistory" class="empty-state">
          当前会话还没有消息。
          <span v-if="activeTargetType === 'group'">这版群聊主区已经能读历史和收实时消息。</span>
          <span v-else>你现在发出去的第一条消息，会直接出现在这里。</span>
        </div>

        <MessageBubble
          v-for="item in activeMessages"
          :key="item.dedupeKey"
          :item="item"
          :peer-name="resolveMessagePeerName(item)"
        />
      </div>

      <MessagesComposer
        :active-peer-uid="activeTargetType === 'single' ? activePeerUid : ''"
        :composer-enabled="(activeTargetType === 'single' && !!activePeerUid) || (activeTargetType === 'group' && !!activeGroupId)"
        :draft-images="draftImages"
        :has-uploading-images="hasUploadingImages"
        :has-failed-images="hasFailedImages"
        :upload-error="uploadError"
        :can-send="canSend"
        :message-draft="messageDraft"
        @update:message-draft="setMessageDraft"
        @send="sendMessage"
        @select-images="uploadImages"
        @remove-draft-image="removeDraftImage"
      />
    </section>

    <MessagesDebugPanel :event-logs="eventLogs" />
  </section>
</template>

<style scoped>
.messages-page {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 20px;
}

.messages-main {
  padding: 22px;
}

.chat-header {
  padding-bottom: 18px;
  border-bottom: 1px solid var(--line);
}

.chat-peer {
  display: flex;
  align-items: center;
  gap: 14px;
}

.chat-peer h2 {
  margin: 0 0 6px;
  font-family: var(--font-heading);
}

.chat-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  object-fit: cover;
}

.chat-avatar.fallback {
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, var(--pink) 0%, #ff9ab6 100%);
  color: #fff;
  font-weight: 800;
}

.message-stream {
  min-height: 460px;
  max-height: 460px;
  overflow-y: auto;
  padding: 22px 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.history-actions {
  display: flex;
  justify-content: center;
}

@media (max-width: 1120px) {
  .messages-page {
    grid-template-columns: 1fr;
  }
}

.messages-page :deep(.messages-debug) {
  grid-column: 1 / -1;
}

@media (max-width: 720px) {
  .messages-main {
    padding: 16px;
  }
}
</style>
