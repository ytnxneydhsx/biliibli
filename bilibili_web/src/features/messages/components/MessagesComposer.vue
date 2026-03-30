<script setup lang="ts">
import type { DraftImageItem } from '../types'

defineProps<{
  activePeerUid: string
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
        :disabled="!activePeerUid"
        @input="emit('update:messageDraft', ($event.target as HTMLTextAreaElement).value)"
      />

      <div class="composer-toolbar">
        <label class="secondary-button upload-button" :class="{ disabled: !activePeerUid }">
          添加图片
          <input
            class="hidden-input"
            type="file"
            accept="image/png,image/jpeg,image/webp"
            multiple
            :disabled="!activePeerUid"
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
  grid-template-columns: minmax(0, 1fr) 110px;
  gap: 12px;
  padding-top: 18px;
  border-top: 1px solid var(--line);
}

.composer-main {
  display: grid;
  gap: 12px;
}

.composer textarea {
  min-height: 84px;
  resize: vertical;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid var(--line);
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
  border-radius: 16px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.82);
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
  color: var(--muted);
}

.ghost-button {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--blue);
  text-align: left;
}

.danger-text {
  color: var(--danger);
}

.hidden-input {
  display: none;
}

.upload-button {
  position: relative;
  overflow: hidden;
  cursor: pointer;
}

.upload-button.disabled {
  pointer-events: none;
  opacity: 0.58;
}

@media (max-width: 720px) {
  .composer {
    grid-template-columns: 1fr;
  }
}
</style>
