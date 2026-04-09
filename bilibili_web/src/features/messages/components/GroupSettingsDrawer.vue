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
  currentUserRole: number
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
  updateMemberRole: [userId: string, role: number]
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

function canChangeMemberRole(role?: number) {
  return props.currentUserRole === 1 && Number(role || 0) !== 1
}
</script>

<template>
  <aside v-if="visible" class="group-settings-drawer panel">
    <div class="drawer-shell">
      <header class="drawer-header">
        <div>
          <span class="drawer-eyebrow">群聊管理</span>
          <h3>群设置</h3>
          <p class="muted">不离开聊天室，直接整理群资料、成员和群规则。</p>
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

      <div class="drawer-body">
        <section v-if="activeTab === 'profile'" class="drawer-section">
          <article class="drawer-panel profile-hero">
            <div class="avatar-row">
              <img v-if="groupAvatar" class="group-avatar" :src="groupAvatar" alt="群头像" />
              <div v-else class="group-avatar fallback">{{ (groupNameDraft || '群').slice(0, 1) }}</div>
              <div class="avatar-copy">
                <strong>群头像</strong>
                <p class="muted">头像会出现在群会话列表和聊天头部，建议保持简洁醒目。</p>
              </div>
            </div>
            <label class="secondary-button upload-button" :class="{ disabled: groupAvatarUploading }">
              {{ groupAvatarUploading ? '上传中…' : '更换群头像' }}
              <input class="hidden-input" type="file" accept="image/*" :disabled="groupAvatarUploading" @change="onAvatarChange" />
            </label>
          </article>

          <form class="drawer-panel profile-form" @submit.prevent="emit('saveGroupName')">
            <div class="panel-copy">
              <strong>群名称</strong>
              <p class="muted">改一个更容易被成员识别的名字，聊天页会立刻同步更新。</p>
            </div>
            <label class="field">
              <span>当前名称</span>
              <input
                type="text"
                :value="groupNameDraft"
                maxlength="30"
                placeholder="给群聊起一个名字"
                @input="emit('update:groupNameDraft', ($event.target as HTMLInputElement).value)"
              />
            </label>
            <div class="panel-actions">
              <button class="primary-button" type="submit" :disabled="groupNameSaving">
                {{ groupNameSaving ? '保存中…' : '保存群资料' }}
              </button>
            </div>
          </form>

          <p v-if="profileError" class="drawer-error">{{ profileError }}</p>
        </section>

        <section v-else-if="activeTab === 'members'" class="drawer-section">
          <article class="drawer-panel">
            <div class="panel-copy">
              <strong>邀请成员</strong>
              <p class="muted">按 UID 邀请成员加入当前群聊，成功后成员列表会立即刷新。</p>
            </div>
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
          </article>

          <p v-if="inviteGroupMemberError" class="drawer-error">{{ inviteGroupMemberError }}</p>
          <p v-if="groupMembersError" class="drawer-error">{{ groupMembersError }}</p>

          <article class="drawer-panel member-list-shell">
            <div class="panel-copy">
              <strong>成员列表</strong>
              <p class="muted">左侧看成员信息，右侧做踢人和单人禁言，列表会在当前区域内滚动。</p>
            </div>

            <div v-if="groupMembersLoading" class="member-list-state muted">成员加载中…</div>

            <div v-else class="member-list">
              <article v-for="member in members" :key="String(member.userId || '')" class="member-card">
                <template v-if="displayMember(String(member.userId || ''))">
                  <div class="member-main">
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
                  </div>
                  <div class="member-actions">
                    <button
                      v-if="canChangeMemberRole(member.role)"
                      class="secondary-button member-role-button"
                      type="button"
                      :disabled="actionTargetUserId === String(member.userId || '')"
                      @click="emit('updateMemberRole', String(member.userId || ''), Number(member.role || 0))"
                    >
                      {{ Number(member.role || 0) === 2 ? '撤销管理员' : '设为管理员' }}
                    </button>
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
          </article>
        </section>

        <section v-else class="drawer-section">
          <article class="drawer-panel">
            <div class="panel-copy">
              <strong>群规则</strong>
              <p class="muted">这部分负责统一群发言秩序，打开后普通成员会暂时失去发言权限。</p>
            </div>
          </article>

          <article class="drawer-panel moderation-card">
            <div>
              <strong>全员禁言</strong>
              <p class="muted">适合公告、维护或者需要短时间统一秩序的场景。</p>
            </div>
            <button class="primary-button" type="button" :disabled="groupMuteUpdating" @click="emit('toggleAllMuted')">
              {{ groupMuteUpdating ? '提交中…' : Number(isAllMuted || 0) === 1 ? '取消全员禁言' : '开启全员禁言' }}
            </button>
          </article>

          <p v-if="moderationError" class="drawer-error">{{ moderationError }}</p>
        </section>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.group-settings-drawer {
  width: clamp(400px, 32vw, 480px);
  min-height: 100%;
  border-left: 1px solid var(--line);
  padding: 0;
  background:
    radial-gradient(circle at top right, rgba(255, 178, 205, 0.28), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(252, 247, 255, 0.96));
}

.drawer-shell {
  height: 100%;
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  min-height: 0;
}

.drawer-header,
.avatar-row,
.invite-row,
.member-main,
.member-actions,
.moderation-card,
.panel-actions {
  display: flex;
}

.drawer-header,
.moderation-card,
.panel-actions {
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 22px 22px 0;
}

.drawer-eyebrow {
  display: inline-flex;
  margin-bottom: 8px;
  color: var(--pink);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.drawer-header h3 {
  margin: 0 0 6px;
  font-family: var(--font-heading);
  font-size: 1.5rem;
}

.drawer-tabs {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  padding: 18px 22px 0;
}

.drawer-tab {
  border: 0;
  border-radius: 16px;
  min-width: 0;
  padding: 12px 10px;
  background: rgba(255, 255, 255, 0.82);
  color: var(--text);
  font-weight: 700;
  box-shadow: inset 0 0 0 1px rgba(255, 170, 198, 0.22);
}

.drawer-tab.active {
  background: linear-gradient(135deg, var(--pink) 0%, #ff8bb0 100%);
  color: #fff;
  box-shadow: 0 14px 30px rgba(255, 122, 166, 0.22);
}

.drawer-body {
  min-height: 0;
  overflow-y: auto;
  padding: 18px 22px 22px;
}

.drawer-section,
.profile-form {
  display: grid;
  gap: 16px;
}

.drawer-panel {
  border: 1px solid rgba(255, 189, 214, 0.32);
  border-radius: 24px;
  padding: 18px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 16px 30px rgba(255, 192, 214, 0.12);
}

.panel-copy {
  display: grid;
  gap: 6px;
  margin-bottom: 14px;
}

.profile-hero,
.invite-row,
.member-card {
  display: grid;
  gap: 14px;
}

.avatar-row,
.member-main {
  align-items: center;
  gap: 12px;
}

.group-avatar,
.member-avatar {
  width: 64px;
  height: 64px;
  border-radius: 22px;
  object-fit: cover;
  flex-shrink: 0;
}

.group-avatar.fallback,
.member-avatar.fallback {
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, var(--pink) 0%, #ff9ab6 100%);
  color: #fff;
  font-weight: 800;
}

.avatar-copy,
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

.invite-row {
  grid-template-columns: minmax(0, 1fr) auto;
}

.member-list-shell {
  min-height: 0;
}

.member-list {
  min-height: 0;
  max-height: 440px;
  overflow-y: auto;
  padding-right: 4px;
}

.member-list-state {
  padding: 8px 0;
}

.member-card {
  border: 1px solid var(--line);
  border-radius: 20px;
  padding: 12px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.88), rgba(250, 247, 255, 0.88));
}

.member-copy {
  flex: 1;
  display: grid;
  gap: 4px;
}

.member-actions {
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.self-row {
  padding-left: 2px;
}

.moderation-card {
  align-items: flex-start;
}

.upload-button {
  justify-self: flex-start;
}

.drawer-error {
  color: #b42318;
  margin: 0;
  padding-inline: 4px;
}

@media (max-width: 960px) {
  .group-settings-drawer {
    width: 100%;
    border-left: 0;
    border-top: 1px solid var(--line);
  }

  .drawer-tabs {
    grid-template-columns: 1fr;
  }

  .invite-row {
    grid-template-columns: 1fr;
  }

  .member-actions,
  .panel-actions,
  .moderation-card {
    justify-content: stretch;
  }

  .member-actions > *,
  .panel-actions > *,
  .moderation-card > .primary-button {
    width: 100%;
  }
}
</style>
