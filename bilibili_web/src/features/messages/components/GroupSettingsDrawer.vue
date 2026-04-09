<script setup lang="ts">
import type { GroupMemberItem, GroupSettingsTab } from '../types'

const props = defineProps<{
  visible: boolean
  activeTab: GroupSettingsTab
  groupNameDraft: string
  groupAvatar: string
  groupNameSaving: boolean
  groupAvatarUploading: boolean
  profileError: string
  members: GroupMemberItem[]
  currentUid: string
  inviteGroupMemberUid: string
  inviteGroupMemberLoading: boolean
  inviteGroupMemberError: string
  groupMembersLoading: boolean
  groupMembersError: string
  groupMuteUpdating: boolean
  isAllMuted: number
  moderationError: string
  actionTargetUserId: string
  resolvePeerName: (uid: string) => string
  resolvePeerAvatar: (uid: string) => string
  resolveGroupMemberRole: (role?: number) => string
  resolveGroupMemberStatus: (status?: number) => string
}>()

const emit = defineEmits<{
  close: []
  switchTab: [tab: GroupSettingsTab]
  'update:groupNameDraft': [value: string]
  saveGroupName: []
  uploadGroupAvatar: [files: File[]]
  'update:inviteGroupMemberUid': [value: string]
  inviteGroupMember: []
  toggleAllMuted: []
  toggleMemberMute: [userId: string, isMuted: number]
  kickMember: [userId: string]
}>()

function onAvatarChange(event: Event) {
  const files = Array.from((event.target as HTMLInputElement).files || [])
  if (files.length) {
    emit('uploadGroupAvatar', files)
  }
  ;(event.target as HTMLInputElement).value = ''
}

function displayMember(uid: string) {
  return uid !== props.currentUid
}
</script>

<template>
  <aside v-if="visible" class="group-settings-drawer panel">
    <header class="drawer-header">
      <div>
        <h3>群设置</h3>
        <p class="muted">不离开聊天室，直接管理当前群聊。</p>
      </div>
      <button class="text-button" type="button" @click="emit('close')">关闭</button>
    </header>

    <nav class="drawer-tabs">
      <button
        class="drawer-tab"
        :class="{ active: activeTab === 'profile' }"
        type="button"
        @click="emit('switchTab', 'profile')"
      >
        群资料
      </button>
      <button
        class="drawer-tab"
        :class="{ active: activeTab === 'members' }"
        type="button"
        @click="emit('switchTab', 'members')"
      >
        成员管理
      </button>
      <button
        class="drawer-tab"
        :class="{ active: activeTab === 'moderation' }"
        type="button"
        @click="emit('switchTab', 'moderation')"
      >
        群管理
      </button>
    </nav>

    <section v-if="activeTab === 'profile'" class="drawer-section">
      <div class="avatar-row">
        <img v-if="groupAvatar" class="group-avatar" :src="groupAvatar" alt="群头像" />
        <div v-else class="group-avatar fallback">{{ (groupNameDraft || '群').slice(0, 1) }}</div>
        <label class="secondary-button upload-button" :class="{ disabled: groupAvatarUploading }">
          {{ groupAvatarUploading ? '上传中…' : '更换群头像' }}
          <input class="hidden-input" type="file" accept="image/*" :disabled="groupAvatarUploading" @change="onAvatarChange" />
        </label>
      </div>

      <form class="profile-form" @submit.prevent="emit('saveGroupName')">
        <label class="field">
          <span>群名称</span>
          <input
            type="text"
            :value="groupNameDraft"
            maxlength="30"
            placeholder="给群聊起一个名字"
            @input="emit('update:groupNameDraft', ($event.target as HTMLInputElement).value)"
          />
        </label>
        <button class="primary-button" type="submit" :disabled="groupNameSaving">
          {{ groupNameSaving ? '保存中…' : '保存群资料' }}
        </button>
      </form>

      <p v-if="profileError" class="drawer-error">{{ profileError }}</p>
    </section>

    <section v-else-if="activeTab === 'members'" class="drawer-section">
      <form class="invite-row" @submit.prevent="emit('inviteGroupMember')">
        <input
          :value="inviteGroupMemberUid"
          type="text"
          inputmode="numeric"
          placeholder="输入要邀请的 UID"
          @input="emit('update:inviteGroupMemberUid', ($event.target as HTMLInputElement).value)"
        />
        <button class="primary-button" type="submit" :disabled="inviteGroupMemberLoading">
          {{ inviteGroupMemberLoading ? '邀请中…' : '邀请成员' }}
        </button>
      </form>

      <p v-if="inviteGroupMemberError" class="drawer-error">{{ inviteGroupMemberError }}</p>
      <p v-if="groupMembersError" class="drawer-error">{{ groupMembersError }}</p>
      <div v-if="groupMembersLoading" class="muted">成员加载中…</div>

      <div v-else class="member-list">
        <article v-for="member in members" :key="String(member.userId || '')" class="member-card">
          <template v-if="displayMember(String(member.userId || ''))">
            <img
              v-if="resolvePeerAvatar(String(member.userId || ''))"
              class="member-avatar"
              :src="resolvePeerAvatar(String(member.userId || ''))"
              :alt="resolvePeerName(String(member.userId || ''))"
            />
            <div v-else class="member-avatar fallback">
              {{ resolvePeerName(String(member.userId || '')).slice(0, 1) }}
            </div>
            <div class="member-copy">
              <strong>{{ resolvePeerName(String(member.userId || '')) }}</strong>
              <span class="muted">
                {{ resolveGroupMemberRole(member.role) }} · {{ resolveGroupMemberStatus(member.status) }}
                <template v-if="Number(member.isMuted || 0) === 1"> · 已禁言</template>
              </span>
            </div>
            <div class="member-actions">
              <button
                class="secondary-button member-action-button"
                type="button"
                :disabled="actionTargetUserId === String(member.userId || '')"
                @click="emit('toggleMemberMute', String(member.userId || ''), Number(member.isMuted || 0))"
              >
                {{ Number(member.isMuted || 0) === 1 ? '取消禁言' : '禁言成员' }}
              </button>
              <button
                class="secondary-button danger-button member-kick-button"
                type="button"
                :disabled="actionTargetUserId === String(member.userId || '')"
                @click="emit('kickMember', String(member.userId || ''))"
              >
                移出群聊
              </button>
            </div>
          </template>
          <template v-else>
            <div class="member-copy self-row">
              <strong>{{ resolvePeerName(String(member.userId || '')) }}</strong>
              <span class="muted">你当前正在管理这个群聊</span>
            </div>
          </template>
        </article>
      </div>
    </section>

    <section v-else class="drawer-section">
      <article class="moderation-card">
        <div>
          <strong>全员禁言</strong>
          <p class="muted">打开后，普通成员将不能继续在当前群聊发言。</p>
        </div>
        <button class="primary-button" type="button" :disabled="groupMuteUpdating" @click="emit('toggleAllMuted')">
          {{ groupMuteUpdating ? '提交中…' : Number(isAllMuted || 0) === 1 ? '取消全员禁言' : '开启全员禁言' }}
        </button>
      </article>
      <p v-if="moderationError" class="drawer-error">{{ moderationError }}</p>
    </section>
  </aside>
</template>

<style scoped>
.group-settings-drawer {
  width: min(360px, 100%);
  min-height: 100%;
  border-left: 1px solid var(--line);
  padding: 20px;
  display: grid;
  align-content: start;
  gap: 18px;
}

.drawer-header,
.drawer-tabs,
.avatar-row,
.invite-row,
.member-card,
.member-actions,
.moderation-card {
  display: flex;
}

.drawer-header,
.moderation-card {
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.drawer-header h3 {
  margin: 0 0 6px;
  font-family: var(--font-heading);
}

.drawer-tabs {
  gap: 10px;
  flex-wrap: wrap;
}

.drawer-tab {
  border: 0;
  border-radius: 999px;
  padding: 8px 14px;
  background: rgba(255, 255, 255, 0.78);
  color: var(--text);
  font-weight: 700;
}

.drawer-tab.active {
  background: var(--pink);
  color: #fff;
}

.drawer-section {
  display: grid;
  gap: 16px;
}

.avatar-row,
.invite-row,
.member-card {
  align-items: center;
  gap: 12px;
}

.group-avatar,
.member-avatar {
  width: 56px;
  height: 56px;
  border-radius: 18px;
  object-fit: cover;
}

.group-avatar.fallback,
.member-avatar.fallback {
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, var(--pink) 0%, #ff9ab6 100%);
  color: #fff;
  font-weight: 800;
}

.profile-form,
.field,
.member-list {
  display: grid;
  gap: 12px;
}

.field span {
  font-weight: 700;
}

.field input,
.invite-row input {
  width: 100%;
}

.member-card {
  justify-content: space-between;
  border: 1px solid var(--line);
  border-radius: 18px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.72);
}

.member-copy {
  flex: 1;
  display: grid;
  gap: 4px;
}

.member-actions {
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.self-row {
  padding-left: 4px;
}

.moderation-card {
  border: 1px solid var(--line);
  border-radius: 18px;
  padding: 14px;
  background: rgba(255, 255, 255, 0.72);
}

.drawer-error {
  color: #b42318;
  margin: 0;
}

@media (max-width: 960px) {
  .group-settings-drawer {
    width: 100%;
    border-left: 0;
    border-top: 1px solid var(--line);
  }
}
</style>
