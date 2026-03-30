<script setup lang="ts">
import MessagesDebugPanel from '../features/messages/components/MessagesDebugPanel.vue'
import MessageBubble from '../features/messages/components/MessageBubble.vue'
import MessagesComposer from '../features/messages/components/MessagesComposer.vue'
import MessagesSidebar from '../features/messages/components/MessagesSidebar.vue'
import { useMessagesPage } from '../features/messages/composables/useMessagesPage'

const {
  activeConversation,
  activeConversationSubtitle,
  activeConversationTitle,
  activeMessages,
  activePeerProfile,
  activePeerUid,
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
  removeDraftImage,
  resolvePeerAvatar,
  resolvePeerName,
  sendMessage,
  setMessageDraft,
  sortedConversations,
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
      :conversations="sortedConversations"
      :active-peer-uid="activePeerUid"
      :resolve-peer-name="resolvePeerName"
      :resolve-peer-avatar="resolvePeerAvatar"
      @reconnect="connectSocket"
      @disconnect="disconnectSocket"
      @open="openConversation"
    />

    <section class="messages-main panel">
      <header class="chat-header">
        <div class="chat-peer">
          <img
            v-if="activePeerProfile?.avatar"
            class="chat-avatar"
            :src="activePeerProfile.avatar"
            :alt="activePeerProfile.nickname"
          />
          <div v-else class="chat-avatar fallback">
            {{ activePeerUid ? resolvePeerName(activePeerUid).slice(0, 1) : '私' }}
          </div>
          <div>
            <h2>{{ activeConversationTitle }}</h2>
            <p class="muted">{{ activeConversationSubtitle }}</p>
          </div>
        </div>
      </header>

      <div ref="messageStream" class="message-stream">
        <div v-if="activePeerUid && activeConversation?.hasMoreHistory" class="history-actions">
          <button class="secondary-button" type="button" :disabled="loadingHistory" @click="loadOlderMessages">
            {{ loadingHistory ? '加载中…' : '加载更早消息' }}
          </button>
        </div>

        <div v-if="!activePeerUid" class="empty-state">
          现在这页已经拆成了页面壳子、消息状态和展示组件。先去任意用户主页点“私信”，我们就能直接进入对应会话。
        </div>
        <div v-else-if="!activeMessages.length && !loadingHistory" class="empty-state">
          当前会话还没有消息。你现在发出去的第一条消息，会直接出现在这里。
        </div>

        <MessageBubble
          v-for="item in activeMessages"
          :key="item.dedupeKey"
          :item="item"
          :peer-name="resolvePeerName(item.peerUid)"
        />
      </div>

      <MessagesComposer
        :active-peer-uid="activePeerUid"
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
