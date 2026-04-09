<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '../lib/api'
import type { UserPrivacySettingVO } from '../types'

type PrivacyOption = {
  value: number
  title: string
  description: string
}

const options: PrivacyOption[] = [
  { value: 1, title: '所有人都可以私信我', description: '任何用户都可以直接给你发私信。' },
  { value: 2, title: '仅联系人可以私信我', description: '只有已经建立联系的人才能直接发消息。' },
  { value: 3, title: '陌生人只能先发一条', description: '陌生用户可以先打一次招呼，后续再决定是否继续。' },
  { value: 4, title: '不接受私信', description: '关闭新的私信入口，避免被陌生消息打扰。' },
]

const loading = ref(true)
const saving = ref(false)
const selectedPolicy = ref(1)
const message = ref('')

async function loadPrivacySetting() {
  loading.value = true
  message.value = ''

  try {
    const setting = await api.get<UserPrivacySettingVO>('/me/im/privacy', undefined)
    selectedPolicy.value = setting.privateMessagePolicy || 1
  } catch (err) {
    message.value = err instanceof Error ? err.message : '隐私设置加载失败'
  } finally {
    loading.value = false
  }
}

async function savePrivacySetting() {
  saving.value = true
  message.value = ''

  try {
    await api.put<void>('/me/im/privacy', {
      privateMessagePolicy: selectedPolicy.value,
    })
    message.value = '隐私设置已更新'
  } catch (err) {
    message.value = err instanceof Error ? err.message : '隐私设置更新失败'
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadPrivacySetting()
})
</script>

<template>
  <section class="privacy-layout">
    <article class="panel privacy-card">
      <div class="privacy-heading">
        <div>
          <p class="privacy-eyebrow">隐私设置</p>
          <h2>决定谁可以先给你发私信。</h2>
          <p class="privacy-copy">这部分会直接保存到你后端已经有的私信隐私设置里，不会新增别的假开关。</p>
        </div>
      </div>

      <p v-if="loading" class="status-text">正在读取当前设置...</p>

      <div v-else class="privacy-options">
        <label
          v-for="option in options"
          :key="option.value"
          class="privacy-option"
          :class="{ active: selectedPolicy === option.value }"
        >
          <input
            v-model="selectedPolicy"
            type="radio"
            name="private-message-policy"
            :value="option.value"
          />
          <div>
            <strong>{{ option.title }}</strong>
            <p>{{ option.description }}</p>
          </div>
        </label>
      </div>

      <div class="action-row">
        <button class="primary-button" type="button" :disabled="loading || saving" @click="savePrivacySetting">
          {{ saving ? '保存中...' : '保存隐私设置' }}
        </button>
        <span class="status-text">{{ message }}</span>
      </div>
    </article>
  </section>
</template>

<style scoped>
.privacy-layout {
  display: grid;
  gap: 24px;
}

.privacy-card {
  padding: 28px;
  display: grid;
  gap: 24px;
}

.privacy-heading {
  display: grid;
  gap: 10px;
}

.privacy-eyebrow {
  margin: 0;
  color: #6b8bff;
  font-size: 0.95rem;
  font-weight: 700;
}

.privacy-heading h2 {
  margin: 8px 0 12px;
  font-size: clamp(1.8rem, 3vw, 2.4rem);
  line-height: 1.1;
}

.privacy-copy {
  margin: 0;
  max-width: 44rem;
  color: rgba(33, 52, 79, 0.72);
  line-height: 1.7;
}

.privacy-options {
  display: grid;
  gap: 14px;
}

.privacy-option {
  display: grid;
  grid-template-columns: 20px minmax(0, 1fr);
  gap: 14px;
  align-items: start;
  padding: 18px;
  border-radius: 20px;
  border: 1px solid rgba(127, 168, 214, 0.2);
  background: rgba(255, 255, 255, 0.86);
  cursor: pointer;
}

.privacy-option.active {
  border-color: rgba(107, 139, 255, 0.42);
  box-shadow: 0 12px 26px rgba(107, 139, 255, 0.12);
}

.privacy-option input {
  margin-top: 4px;
}

.privacy-option strong {
  display: block;
  color: #21344f;
  font-size: 1rem;
}

.privacy-option p {
  margin: 8px 0 0;
  color: rgba(33, 52, 79, 0.72);
  line-height: 1.65;
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

@media (max-width: 720px) {
  .privacy-card {
    padding: 22px;
  }
}
</style>
