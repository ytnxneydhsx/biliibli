<script setup lang="ts">
import type { DraftImageItem } from '../types'

defineProps<{
  activePeerUid: string
  composerEnabled: boolean
  draftImages: DraftImageItem[]
  hasUploadingImages: boolean
  hasFailedImages: boolean
  uploadError: string
  canSend: boolean
  messageDraft: string
}>()

const emit = defineEmits<{
  'update:messageDraft': [value: string]
  send: []
  'select-images': [files: File[]]
  'remove-draft-image': [localId: string]
}>()

function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement | null
  const files = Array.from(input?.files || [])
  if (!files.length) {
    return
  }
  emit('select-images', files)
  if (input) {
    input.value = ''
  }
}
</script>

<template>
  <form class="composer" @submit.prevent="emit('send')">
    <div class="composer-main">
      <div v-if="draftImages.length" class="draft-images">
        <article v-for="item in draftImages" :key="item.localId" class="draft-image-card">
          <img class="draft-image" :src="item.previewUrl" alt="待发送图片" />
          <div class="draft-image-meta">
            <span v-if="item.uploading">上传中…</span>
            <span v-else-if="item.error" class="danger-text">{{ item.error }}</span>
            <span v-else>已上传</span>
            <button class="ghost-button" type="button" @click="emit('remove-draft-image', item.localId)">移除</button>
          </div>
        </article>
      </div>

      <textarea
        :model-value="messageDraft"
        placeholder="发送一条文字、图片或图文消息"
        :disabled="!composerEnabled"
        @input="emit('update:messageDraft', ($event.target as HTMLTextAreaElement).value)"
      />

      <div class="composer-toolbar">
        <label class="secondary-button upload-button" :class="{ disabled: !composerEnabled }">
          添加图片
          <input
            class="hidden-input"
            type="file"
            accept="image/png,image/jpeg,image/webp"
            multiple
            :disabled="!composerEnabled"
            @change="onFileChange"
          />
        </label>
        <span v-if="hasUploadingImages" class="muted">图片上传中，完成后才能发送。</span>
        <span v-else-if="hasFailedImages || uploadError" class="danger-text">
          {{ uploadError || '有图片上传失败，请移除或重试。' }}
        </span>
      </div>
    </div>

    <button class="primary-button" type="submit" :disabled="!canSend">发送</button>
  </form>
</template>

<style scoped>
.composer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 72px;
  gap: 10px;
  padding: 12px 14px;
  border-top: 1px solid var(--bili-line, #e3e5e7);
  background: rgba(16, 27, 41, 0.94);
  align-items: end;
}

.composer-main {
  display: grid;
  gap: 12px;
}

.composer textarea {
  min-height: 42px;
  height: 42px;
  resize: vertical;
  padding: 10px 12px;
  border-radius: 14px;
  border: 1px solid var(--bili-line, #e3e5e7);
  background: rgba(255, 255, 255, 0.08);
  color: var(--bili-text, #f7fbff);
  outline: none;
  box-shadow: none;
}

.composer textarea::placeholder {
  color: var(--bili-subtle, #9499a0);
}

.composer textarea:focus {
  border-color: var(--bili-blue, #00aeec);
  box-shadow: 0 0 0 2px rgba(50, 197, 255, 0.14);
}

.composer > .primary-button {
  width: 72px;
  min-height: 42px;
  align-self: end;
  border-radius: 999px;
  background: linear-gradient(135deg, var(--bili-pink, #fb7299) 0%, #ff5f8f 100%);
  box-shadow: 0 16px 32px rgba(251, 114, 153, 0.24);
}

.composer > .primary-button:hover:not(:disabled) {
  background: linear-gradient(135deg, #ff83a7 0%, #ff5f8f 100%);
}

.composer-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.draft-images {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
  gap: 12px;
}

.draft-image-card {
  display: grid;
  gap: 8px;
  padding: 10px;
  border-radius: 14px;
  border: 1px solid var(--bili-line, #e3e5e7);
  background: rgba(255, 255, 255, 0.07);
}

.draft-image {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: 12px;
}

.draft-image-meta {
  display: grid;
  gap: 6px;
  font-size: 12px;
  color: var(--bili-subtle, #9499a0);
}

.ghost-button {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--bili-blue, #00aeec);
  text-align: left;
}

.danger-text {
  color: var(--bili-pink, #fb7299);
}

.hidden-input {
  display: none;
}

.upload-button {
  position: relative;
  overflow: hidden;
  cursor: pointer;
  min-height: 30px;
  padding: 0 10px;
  border: 1px solid var(--line-strong, rgba(187, 208, 235, 0.2));
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.07);
  color: var(--bili-text, #f7fbff);
  box-shadow: none;
}

.upload-button.disabled {
  pointer-events: none;
  opacity: 0.58;
}

@media (max-width: 720px) {
  .composer {
    grid-template-columns: 1fr 64px;
  }
}
</style>
