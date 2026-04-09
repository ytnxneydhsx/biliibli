<script setup lang="ts">
import { reactive, ref } from 'vue'
import { api } from '../lib/api'
import type {
  VideoUploadCompleteVO,
  VideoUploadInitVO,
  VideoUploadPartSignVO,
} from '../types'

const coverMessage = ref('')
const uploadMessage = ref('')
const coverUrl = ref('')
const selectedVideo = ref<File | null>(null)
const selectedVideoDuration = ref(0)
const uploadProgress = ref(0)
const uploadBusy = ref(false)
const currentUploadId = ref('')

const uploadForm = reactive({
  title: '',
  description: '',
})

async function uploadCover(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) {
    return
  }

  const formData = new FormData()
  formData.append('file', file)

  try {
    const url = await api.post<string>('/me/uploads/video-cover', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    coverUrl.value = url
    coverMessage.value = '封面上传成功'
  } catch (err) {
    coverMessage.value = err instanceof Error ? err.message : '上传失败'
  } finally {
    input.value = ''
  }
}

async function probeDuration(file: File) {
  const url = URL.createObjectURL(file)
  try {
    const duration = await new Promise<number>((resolve, reject) => {
      const video = document.createElement('video')
      video.preload = 'metadata'
      video.onloadedmetadata = () => resolve(Math.floor(video.duration || 0))
      video.onerror = () => reject(new Error('读取视频时长失败'))
      video.src = url
    })
    selectedVideoDuration.value = duration
  } catch {
    selectedVideoDuration.value = 0
  } finally {
    URL.revokeObjectURL(url)
  }
}

async function onVideoSelected(event: Event) {
  const input = event.target as HTMLInputElement
  selectedVideo.value = input.files?.[0] || null
  if (selectedVideo.value) {
    await probeDuration(selectedVideo.value)
  } else {
    selectedVideoDuration.value = 0
  }
}

async function uploadToSignedUrl(url: string, blob: Blob) {
  const response = await fetch(url, {
    method: 'PUT',
    body: blob,
  })

  if (!response.ok) {
    throw new Error(`分片上传失败：${response.status}`)
  }

  const etag = response.headers.get('etag') || response.headers.get('ETag')
  if (!etag) {
    throw new Error('分片上传缺少 ETag')
  }

  return etag
}

async function publishVideo() {
  if (!selectedVideo.value) {
    uploadMessage.value = '请先选择视频文件'
    return
  }
  if (!uploadForm.title.trim()) {
    uploadMessage.value = '标题不能为空'
    return
  }

  uploadBusy.value = true
  uploadMessage.value = ''
  uploadProgress.value = 0
  currentUploadId.value = ''

  try {
    const file = selectedVideo.value
    const init = await api.post<VideoUploadInitVO>('/me/videos/uploads/init-session', {
      fileName: file.name,
      totalSize: file.size,
      chunkSize: 0,
      totalChunks: 0,
      contentType: file.type,
      fileMd5: '',
    })

    currentUploadId.value = init.uploadId
    const partNumbers = Array.from({ length: init.totalChunks }, (_, index) => index + 1)
    const signed = await api.post<VideoUploadPartSignVO>(`/me/videos/uploads/${init.uploadId}/parts/sign`, {
      partNumbers,
    })

    const etags: Array<{ partNumber: number; etag: string }> = []

    for (const part of signed.parts) {
      const start = (part.partNumber - 1) * init.chunkSize
      const end = Math.min(file.size, start + init.chunkSize)
      const blob = file.slice(start, end)
      const etag = await uploadToSignedUrl(part.uploadUrl, blob)
      etags.push({ partNumber: part.partNumber, etag })
      uploadProgress.value = Math.round((etags.length / signed.parts.length) * 100)
    }

    etags.sort((left, right) => left.partNumber - right.partNumber)

    const result = await api.post<VideoUploadCompleteVO>(`/me/videos/uploads/${init.uploadId}/complete`, {
      title: uploadForm.title.trim(),
      description: uploadForm.description.trim(),
      coverUrl: coverUrl.value || null,
      duration: selectedVideoDuration.value,
      parts: etags,
    })

    uploadMessage.value = `视频发布成功，videoId=${result.videoId}`
    uploadForm.title = ''
    uploadForm.description = ''
    coverUrl.value = ''
    selectedVideo.value = null
    selectedVideoDuration.value = 0
    currentUploadId.value = ''
  } catch (err) {
    uploadMessage.value = err instanceof Error ? err.message : '视频上传失败'
    if (currentUploadId.value) {
      api.delete<void>(`/me/videos/uploads/${currentUploadId.value}`).catch(() => undefined)
    }
  } finally {
    uploadBusy.value = false
  }
}
</script>

<template>
  <section class="studio-layout">
    <article class="panel studio-block">
      <div class="studio-heading">
        <p class="studio-eyebrow">创作中心</p>
        <h1>把新作品整理好，再发给大家看。</h1>
        <p class="studio-copy">这里专心负责上传内容。头像、昵称和签名已经单独拆开，不会再和发布流程混在一起。</p>
      </div>

      <div class="field-grid">
        <div class="field-group">
          <label for="video-title">标题</label>
          <input id="video-title" v-model.trim="uploadForm.title" maxlength="100" />
        </div>

        <div class="field-group">
          <label for="video-desc">简介</label>
          <textarea id="video-desc" v-model.trim="uploadForm.description" />
        </div>

        <div class="field-group">
          <label>视频封面</label>
          <div class="status-line">
            <label class="secondary-button upload-picker">
              上传封面
              <input type="file" accept="image/*" @change="uploadCover" />
            </label>
            <span class="muted">{{ coverMessage }}</span>
          </div>
          <img v-if="coverUrl" class="cover-preview" :src="coverUrl" alt="cover preview" />
        </div>

        <div class="field-group">
          <label>视频文件</label>
          <label class="secondary-button upload-picker">
            选择视频
            <input type="file" accept="video/*" @change="onVideoSelected" />
          </label>
          <div v-if="selectedVideo" class="status-line">
            <span>{{ selectedVideo.name }}</span>
            <span>{{ (selectedVideo.size / 1024 / 1024).toFixed(2) }} MB</span>
            <span>时长 {{ selectedVideoDuration }} 秒</span>
          </div>
        </div>

        <div v-if="uploadBusy || uploadProgress" class="field-group">
          <label>上传进度</label>
          <div class="progress-bar">
            <span :style="{ width: `${uploadProgress}%` }" />
          </div>
          <div class="status-line">
            <span>{{ uploadProgress }}%</span>
            <span v-if="currentUploadId">uploadId={{ currentUploadId }}</span>
          </div>
        </div>

        <div class="status-line">
          <button class="primary-button" type="button" :disabled="uploadBusy" @click="publishVideo">
            {{ uploadBusy ? '正在上传...' : '上传并发布' }}
          </button>
          <span class="muted">{{ uploadMessage }}</span>
        </div>
      </div>
    </article>
  </section>
</template>

<style scoped>
.studio-layout {
  display: grid;
  gap: 24px;
}

.studio-block {
  padding: 28px;
  display: grid;
  gap: 24px;
}

.studio-heading {
  display: grid;
  gap: 10px;
}

.studio-eyebrow {
  margin: 0;
  color: #ff6b9f;
  font-size: 0.95rem;
  font-weight: 700;
}

.studio-heading h1 {
  margin: 0;
  font-size: clamp(1.8rem, 3vw, 2.5rem);
  line-height: 1.1;
}

.studio-copy {
  margin: 0;
  max-width: 48rem;
  color: rgba(33, 52, 79, 0.72);
  line-height: 1.7;
}

.upload-picker {
  position: relative;
  overflow: hidden;
}

.upload-picker input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.cover-preview {
  width: min(280px, 100%);
  aspect-ratio: 16 / 10;
  object-fit: cover;
  border-radius: 16px;
  border: 1px solid var(--line);
}

@media (max-width: 720px) {
  .studio-block {
    padding: 22px;
  }
}
</style>
