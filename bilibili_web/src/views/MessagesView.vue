<script setup lang="ts">
import MessagesDebugPanel from '../features/messages/components/MessagesDebugPanel.vue'
import MessageBubble from '../features/messages/components/MessageBubble.vue'
import MessagesComposer from '../features/messages/components/MessagesComposer.vue'
import GroupSettingsDrawer from '../features/messages/components/GroupSettingsDrawer.vue'
import MessagesSidebar from '../features/messages/components/MessagesSidebar.vue'
import { useMessagesPage } from '../features/messages/composables/useMessagesPage'

const {
  activeConversationSubtitle,
  activeConversationTitle,
  activeGroupId,
  activeGroupMembers,
  activeGroupProfile,
  activeGroupRole,
  actionTargetUserId,
  activeHasMoreHistory,
  activeMessages,
  activePeerProfile,
  activePeerUid,
  activeTargetType,
  canSend,
  canManageActiveGroup,
  closeGroupSettings,
  connectionLabel,
  connectionState,
  connectSocket,
  currentProfile,
  currentUid,
  currentUsername,
  disconnectSocket,
  draftImages,
  eventLogs,
  groupAvatarUploading,
  groupMembersError,
  groupMembersLoading,
  groupMuteUpdating,
  groupNameDraft,
  groupNameSaving,
  groupSettingsTab,
  groupSettingsVisible,
  hasFailedImages,
  hasUploadingImages,
  inviteGroupMember,
  inviteGroupMemberError,
  inviteGroupMemberLoading,
  inviteGroupMemberUid,
  kickGroupMember,
  loadOlderMessages,
  loadingHistory,
  moderationError,
  messageDraft,
  messageStream,
  openConversation,
  openGroupConversation,
  openGroupSettings,
  profileError,
  removeDraftImage,
  resolveGroupAvatar,
  resolveGroupMemberRole,
  resolveGroupMemberStatus,
  resolveGroupName,
  resolveMessagePeerName,
  resolvePeerAvatar,
  resolvePeerName,
  saveGroupName,
  sendMessage,
  setGroupNameDraft,
  setGroupSettingsTab,
  setInviteGroupMemberUid,
  setSidebarTab,
  sidebarTab,
  setMessageDraft,
  sortedGroupConversations,
  sortedSingleConversations,
  toggleGroupMuteStatus,
  toggleMemberMute,
  updateMemberRole,
  uploadError,
  uploadGroupAvatar,
  uploadImages,
} = useMessagesPage()

void messageStream
</script>

<template>
  <section class="messages-page">
    <div class="messages-canvas">
      <header class="messages-topbar">
        <strong>我的消息</strong>
        <span>{{ activeTargetType === 'group' ? '群聊天' : '私人聊天' }}</span>
      </header>

      <div class="messages-layout">
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

    <section class="messages-shell">
      <section class="messages-main">
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
          <button
            v-if="activeTargetType === 'group' && activeGroupId && canManageActiveGroup"
            class="secondary-button"
            type="button"
            @click="openGroupSettings"
          >
            群设置
          </button>
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
            <span v-if="activeTargetType === 'group'">你可以先发一句话，或者直接在右上角管理这个群聊。</span>
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

      <GroupSettingsDrawer
        :visible="groupSettingsVisible"
        :active-tab="groupSettingsTab"
        :group-name-draft="groupNameDraft"
        :group-avatar="activeGroupProfile?.groupAvatar || ''"
        :group-name-saving="groupNameSaving"
        :group-avatar-uploading="groupAvatarUploading"
        :profile-error="profileError"
        :members="activeGroupMembers"
        :current-uid="currentUid"
        :invite-group-member-uid="inviteGroupMemberUid"
        :invite-group-member-loading="inviteGroupMemberLoading"
        :invite-group-member-error="inviteGroupMemberError"
        :group-members-loading="groupMembersLoading"
        :group-members-error="groupMembersError"
        :group-mute-updating="groupMuteUpdating"
        :is-all-muted="Number(activeGroupProfile?.isAllMuted || 0)"
        :moderation-error="moderationError"
        :action-target-user-id="actionTargetUserId"
        :current-user-role="activeGroupRole"
        :resolve-peer-name="resolvePeerName"
        :resolve-peer-avatar="resolvePeerAvatar"
        :resolve-group-member-role="resolveGroupMemberRole"
        :resolve-group-member-status="resolveGroupMemberStatus"
        @close="closeGroupSettings"
        @switch-tab="setGroupSettingsTab"
        @update:group-name-draft="setGroupNameDraft"
        @save-group-name="saveGroupName"
        @upload-group-avatar="uploadGroupAvatar"
        @update:invite-group-member-uid="setInviteGroupMemberUid"
        @invite-group-member="inviteGroupMember"
        @toggle-all-muted="toggleGroupMuteStatus"
        @toggle-member-mute="toggleMemberMute"
        @update-member-role="updateMemberRole"
        @kick-member="kickGroupMember"
      />
    </section>

    <MessagesDebugPanel :event-logs="eventLogs" />
      </div>
    </div>
  </section>
</template>

<style scoped>
.messages-page {
  --bili-bg: var(--bg);
  --bili-panel: var(--bg-panel);
  --bili-panel-strong: var(--bg-panel-strong);
  --bili-text: var(--text);
  --bili-muted: var(--muted);
  --bili-subtle: #6f839b;
  --bili-line: var(--line);
  --bili-line-soft: rgba(165, 188, 218, 0.08);
  --bili-blue: var(--blue);
  --bili-blue-hover: #6fd8ff;
  --bili-blue-soft: var(--blue-soft);
  --bili-pink: var(--pink);
  min-height: 100vh;
  padding: 6px 0;
  background:
    radial-gradient(circle at 9% -8%, rgba(251, 114, 153, 0.2), transparent 28%),
    radial-gradient(circle at 86% 0%, rgba(50, 197, 255, 0.16), transparent 30%),
    linear-gradient(180deg, #07111c 0%, #0a1522 44%, #08111d 100%);
  color: var(--bili-text);
}

.messages-canvas {
  width: min(1020px, calc(100vw - 24px));
  min-height: calc(100vh - 12px);
  margin: 0 auto;
  border-left: 1px solid var(--bili-line);
  border-right: 1px solid var(--bili-line);
  background: rgba(8, 17, 29, 0.58);
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.26);
}

.messages-topbar {
  height: 52px;
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
  padding: 0 16px;
  border: 1px solid var(--bili-line);
  border-top: 0;
  background: rgba(18, 29, 43, 0.86);
  box-shadow: 0 14px 34px rgba(0, 0, 0, 0.18);
  backdrop-filter: blur(20px);
}

.messages-topbar strong {
  font-size: 15px;
}

.messages-topbar span {
  color: var(--bili-subtle);
  font-size: 13px;
}

.messages-layout {
  display: grid;
  grid-template-columns: 400px minmax(0, 1fr);
  align-items: stretch;
  min-height: calc(100vh - 64px);
}

.messages-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 0;
}

.messages-main {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  min-height: calc(100vh - 64px);
  border-radius: 0;
  border: 0;
  background: rgba(10, 21, 34, 0.74);
  box-shadow: none;
  backdrop-filter: none;
}

.chat-header {
  min-height: 56px;
  padding: 0 14px;
  border-bottom: 1px solid var(--bili-line);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.chat-peer {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chat-peer h2 {
  margin: 0 0 2px;
  font-family: var(--font-heading);
  color: var(--bili-text);
  font-size: 14px;
}

.chat-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
}

.chat-avatar.fallback {
  display: grid;
  place-items: center;
  background: var(--bili-blue);
  color: #fff;
  font-weight: 800;
}

.chat-header :deep(.muted),
.chat-peer .muted {
  color: var(--bili-subtle);
  font-size: 12px;
}

.chat-header .secondary-button {
  min-height: 32px;
  padding: 0 12px;
  border: 1px solid var(--line-strong);
  background: rgba(255, 255, 255, 0.07);
  color: var(--bili-text);
  box-shadow: none;
}

.message-stream {
  min-height: 0;
  max-height: none;
  overflow-y: auto;
  padding: 22px 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  scrollbar-color: #c9ccd0 transparent;
}

.message-stream::-webkit-scrollbar {
  width: 8px;
}

.message-stream::-webkit-scrollbar-track {
  background: transparent;
}

.message-stream::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: #c9ccd0;
}

.history-actions {
  display: flex;
  justify-content: center;
}

.history-actions .secondary-button {
  min-height: 30px;
  border: 1px solid var(--line-strong);
  background: rgba(255, 255, 255, 0.07);
  color: var(--bili-text);
  box-shadow: none;
}

.messages-page :deep(.empty-state) {
  border: 1px dashed var(--bili-line);
  background: rgba(255, 255, 255, 0.05);
  color: var(--bili-subtle);
}

@media (max-width: 1120px) {
  .messages-canvas {
    width: min(100vw - 16px, 1020px);
  }

  .messages-layout {
    grid-template-columns: 1fr;
  }
}

.messages-page :deep(.messages-debug) {
  grid-column: 1 / -1;
}

@media (max-width: 720px) {
  .messages-page {
    padding: 0;
    background: var(--bili-bg);
  }

  .messages-canvas {
    width: 100%;
    border: 0;
  }

  .messages-main {
    min-height: 620px;
  }

  .chat-header {
    align-items: flex-start;
  }

  .chat-avatar {
    width: 34px;
    height: 34px;
  }

  .message-stream {
    min-height: 360px;
    padding: 16px 12px;
  }
}
</style>
