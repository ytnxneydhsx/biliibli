<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { authState, refreshCurrentUser } from '../lib/auth'
import { api } from '../lib/api'

const profileForm = reactive({
  nickname: '',
  sign: '',
})

const profileMessage = ref('')
const avatarMessage = ref('')

const currentAvatar = computed(() => authState.profile?.avatar || '')

function syncProfileForm() {
  profileForm.nickname = authState.profile?.nickname || ''
  profileForm.sign = authState.profile?.sign || ''
}

syncProfileForm()

async function saveProfile() {
  profileMessage.value = ''
  try {
    await api.put<void>('/me/profile', profileForm)
    await refreshCurrentUser()
    syncProfileForm()
    profileMessage.value = '资料已更新'
  } catch (err) {
    profileMessage.value = err instanceof Error ? err.message : '资料更新失败'
  }
}

async function uploadAvatar(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) {
    return
  }

  const formData = new FormData()
  formData.append('file', file)

  try {
    await api.post<string>('/me/uploads/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    await refreshCurrentUser()
    avatarMessage.value = '头像上传成功'
  } catch (err) {
    avatarMessage.value = err instanceof Error ? err.message : '上传失败'
  } finally {
    input.value = ''
  }
}
</script>

<template>
  <section class="settings-layout">
    <article class="panel settings-card">
      <div class="settings-heading">
        <div>
          <p class="settings-eyebrow">资料设置</p>
          <h1>把你的主页整理得更像自己</h1>
          <p class="settings-copy">更新头像、昵称和签名，让大家更容易认出你，也更愿意点进你的主页看看。</p>
        </div>
      </div>

      <div class="settings-profile">
        <img v-if="currentAvatar" class="profile-avatar" :src="currentAvatar" alt="avatar" />
        <div v-else class="profile-avatar fallback-avatar">{{ authState.profile?.nickname?.slice(0, 1) || '我' }}</div>
        <div class="profile-summary">
          <strong>{{ authState.profile?.nickname || authState.username || '还没有昵称' }}</strong>
          <span>{{ authState.profile?.sign || '写一句签名，让大家更快记住你。' }}</span>
          <label class="primary-button upload-picker">
            修改头像
            <input type="file" accept="image/*" @change="uploadAvatar" />
          </label>
        </div>
      </div>

      <p v-if="avatarMessage" class="status-text">{{ avatarMessage }}</p>

      <div class="field-grid">
        <div class="field-group">
          <label for="nickname">昵称</label>
          <input id="nickname" v-model.trim="profileForm.nickname" maxlength="24" />
        </div>
        <div class="field-group">
          <label for="sign">签名</label>
          <textarea id="sign" v-model.trim="profileForm.sign" maxlength="120" rows="4" />
        </div>
        <div class="action-row">
          <button class="primary-button" type="button" @click="saveProfile">保存资料</button>
          <span class="status-text">{{ profileMessage }}</span>
        </div>
      </div>
    </article>
  </section>
</template>

<style scoped>
.settings-layout {
  display: grid;
  gap: 24px;
}

.settings-card {
  padding: 28px;
  display: grid;
  gap: 24px;
}

.settings-heading h1 {
  margin: 8px 0 12px;
  font-size: clamp(1.8rem, 3vw, 2.4rem);
  line-height: 1.1;
}

.settings-eyebrow {
  margin: 0;
  color: #ff6b9f;
  font-size: 0.95rem;
  font-weight: 700;
}

.settings-copy {
  margin: 0;
  max-width: 42rem;
  color: rgba(33, 52, 79, 0.72);
  line-height: 1.7;
}

.settings-profile {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  align-items: center;
  padding: 20px;
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(255, 214, 234, 0.45), rgba(201, 238, 255, 0.55));
}

.profile-avatar {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid rgba(255, 255, 255, 0.85);
  box-shadow: 0 14px 30px rgba(92, 136, 194, 0.16);
}

.fallback-avatar {
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #ff8fc4, #72c8ff);
  color: #fff;
  font-size: 2rem;
  font-weight: 700;
}

.profile-summary {
  display: grid;
  gap: 10px;
  color: #21344f;
}

.profile-summary strong {
  font-size: 1.15rem;
}

.profile-summary span {
  color: rgba(33, 52, 79, 0.72);
}

.field-grid {
  display: grid;
  gap: 18px;
}

.field-group {
  display: grid;
  gap: 10px;
}

.field-group label {
  color: #21344f;
  font-weight: 600;
}

.field-group input,
.field-group textarea {
  width: 100%;
  padding: 14px 16px;
  border: 1px solid rgba(127, 168, 214, 0.22);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  color: #21344f;
  font: inherit;
}

.field-group textarea {
  resize: vertical;
  min-height: 120px;
}

.field-group input:focus,
.field-group textarea:focus {
  outline: none;
  border-color: rgba(255, 107, 159, 0.5);
  box-shadow: 0 0 0 4px rgba(255, 107, 159, 0.12);
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  align-items: center;
}

.status-text {
  margin: 0;
  color: rgba(33, 52, 79, 0.72);
}

.upload-picker {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  cursor: pointer;
  width: fit-content;
}

.upload-picker input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

@media (max-width: 720px) {
  .settings-card {
    padding: 22px;
  }

  .settings-profile {
    padding: 18px;
  }
}
</style>
