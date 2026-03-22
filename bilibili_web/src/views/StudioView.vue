<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { authState, refreshCurrentUser } from '../lib/auth'
import { api } from '../lib/api'
import type {
  VideoUploadCompleteVO,
  VideoUploadInitVO,
  VideoUploadPartSignVO,
} from '../types'

const profileForm = reactive({
  nickname: '',
  sign: '',
})

const profileMessage = ref('')
const avatarMessage = ref('')
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

async function uploadSingleImage(event: Event, endpoint: string, target: 'avatar' | 'cover') {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) {
    return
  }

  const formData = new FormData()
  formData.append('file', file)

  try {
    const url = await api.post<string>(endpoint, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    if (target === 'avatar') {
      await refreshCurrentUser()
      avatarMessage.value = '头像上传成功'
    } else {
      coverUrl.value = url
      coverMessage.value = '封面上传成功'
    }
  } catch (err) {
    const message = err instanceof Error ? err.message : '上传失败'
    if (target === 'avatar') {
      avatarMessage.value = message
    } else {
      coverMessage.value = message
    }
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
      <div class="section-title">
        <h2>资料设置 / 头像修改</h2>
      </div>
      <div class="studio-profile">
        <img v-if="currentAvatar" class="profile-avatar" :src="currentAvatar" alt="avatar" />
        <div v-else class="profile-avatar fallback-avatar">{{ authState.profile?.nickname?.slice(0, 1) || '我' }}</div>
        <div class="studio-profile-actions">
          <label class="primary-button upload-picker">
            修改头像
            <input type="file" accept="image/*" @change="uploadSingleImage($event, '/me/uploads/avatar', 'avatar')" />
          </label>
          <span class="muted">支持直接上传头像、修改昵称和个性签名</span>
        </div>
      </div>
      <p v-if="avatarMessage" class="muted">{{ avatarMessage }}</p>

      <div class="field-grid">
        <div class="field-group">
          <label for="nickname">昵称</label>
          <input id="nickname" v-model.trim="profileForm.nickname" />
        </div>
        <div class="field-group">
          <label for="sign">签名</label>
          <textarea id="sign" v-model.trim="profileForm.sign" />
        </div>
        <div class="status-line">
          <button class="primary-button" type="button" @click="saveProfile">保存资料</button>
          <span class="muted">{{ profileMessage }}</span>
        </div>
      </div>
    </article>

    <article class="panel studio-block">
      <div class="section-title">
        <h2>上传视频</h2>
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
              <input type="file" accept="image/*" @change="uploadSingleImage($event, '/me/uploads/video-cover', 'cover')" />
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
  padding: 24px;
}

.studio-profile {
  display: flex;
  gap: 16px;
  align-items: center;
  margin-bottom: 16px;
}

.studio-profile-actions {
  display: grid;
  gap: 10px;
}

.profile-avatar {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  object-fit: cover;
}

.fallback-avatar {
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, var(--pink) 0%, #ff9ab6 100%);
  color: #fff;
  font-size: 34px;
  font-weight: 700;
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
</style>
