<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../lib/api'

type AdminPendingVideoVO = {
  id: string
  authorUid: string
  title: string
  description: string
  coverUrl: string
  videoUrl: string
  duration: number
  createTime: string
  nickname: string
}

type CursorPageVO<T> = {
  records: T[]
  nextCursor: string | null
  hasMore: boolean
}

const videos = ref<AdminPendingVideoVO[]>([])
const loading = ref(false)
const errorMessage = ref('')
const nextCursor = ref<string | null>(null)
const hasMore = ref(false)
const selectedVideoId = ref<string | null>(null)

const selectedVideo = computed(() =>
  videos.value.find((video) => video.id === selectedVideoId.value) ?? null,
)

const emptyTitle = computed(() => (loading.value ? '正在加载待审核视频…' : '当前没有待审核视频'))
const emptyDescription = computed(() =>
  loading.value ? '请稍候，管理员列表正在从后端拉取数据。' : '新提交的视频会先进入待审核队列。',
)

function formatDuration(seconds: number) {
  if (!Number.isFinite(seconds) || seconds < 0) {
    return '--'
  }

  const totalSeconds = Math.floor(seconds)
  const minutes = Math.floor(totalSeconds / 60)
  const remainingSeconds = totalSeconds % 60
  return `${minutes}:${String(remainingSeconds).padStart(2, '0')}`
}

async function loadPendingVideos() {
  loading.value = true
  errorMessage.value = ''

  try {
    const result = await api.get<CursorPageVO<AdminPendingVideoVO>>('/admin/videos/pending')
    videos.value = result.records ?? []
    nextCursor.value = result.nextCursor ?? null
    hasMore.value = Boolean(result.hasMore)
    if (!videos.value.length) {
      selectedVideoId.value = null
    } else if (!selectedVideoId.value || !videos.value.some((video) => video.id === selectedVideoId.value)) {
      selectedVideoId.value = videos.value[0].id
    }
  } catch (error) {
    errorMessage.value = (error as Error).message
    videos.value = []
    nextCursor.value = null
    hasMore.value = false
    selectedVideoId.value = null
  } finally {
    loading.value = false
  }
}

function openVideoReview(videoId: string) {
  selectedVideoId.value = videoId
}

async function reviewVideo(status: 0 | 1) {
  if (!selectedVideo.value) {
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    await api.put(`/admin/videos/${selectedVideo.value.id}/status`, { status })
    await loadPendingVideos()
  } catch (error) {
    errorMessage.value = (error as Error).message
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadPendingVideos()
})
</script>

<template>
  <section class="page-section">
    <header class="page-header">
      <div>
        <h1>视频管理</h1>
        <p>管理员后台默认首页，当前展示待审核视频列表。</p>
      </div>
    </header>

    <div class="tabs-row">
      <button class="tab-button is-active" type="button">待审核</button>
      <button class="tab-button" type="button" disabled>已通过</button>
      <button class="tab-button" type="button" disabled>已拒绝</button>
    </div>

    <div class="video-layout">
      <section class="panel">
        <div class="panel-header">
          <div>
            <h2>视频列表</h2>
            <p>封面 / 标题 / 投稿人 / 投稿时间 / 时长 / 当前状态 / 操作按钮</p>
          </div>
          <button class="refresh-button" type="button" @click="loadPendingVideos">刷新</button>
        </div>

        <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

        <div v-if="loading || videos.length === 0" class="empty-state">
          <h3>{{ emptyTitle }}</h3>
          <p>{{ emptyDescription }}</p>
        </div>

        <ul v-else class="video-list">
          <li v-for="video in videos" :key="video.id" class="video-item">
            <img class="video-cover" :src="video.coverUrl" :alt="video.title" />
            <div class="video-meta">
              <div class="video-title-row">
                <h3>{{ video.title }}</h3>
                <span class="status-badge">待审核</span>
              </div>
              <p class="video-subtitle">投稿人：{{ video.nickname }} · 投稿时间：{{ video.createTime }}</p>
              <p class="video-subtitle">时长：{{ formatDuration(video.duration) }} · 作者ID：{{ video.authorUid }}</p>
              <p class="video-description">{{ video.description || '暂无简介' }}</p>
              <button class="review-link" type="button" @click="openVideoReview(video.id)">查看审核</button>
            </div>
          </li>
        </ul>

        <p v-if="hasMore" class="hint-text">还有更多待审核视频，后续可接入下一页。</p>
        <p v-if="nextCursor" class="hint-text">下一页游标：{{ nextCursor }}</p>
      </section>

      <aside class="panel">
        <template v-if="selectedVideo">
          <h2>审核抽屉</h2>
          <img class="drawer-cover" :src="selectedVideo.coverUrl" :alt="selectedVideo.title" />
          <h3>{{ selectedVideo.title }}</h3>
          <p class="video-subtitle">投稿人：{{ selectedVideo.nickname }}</p>
          <p class="video-subtitle">投稿时间：{{ selectedVideo.createTime }}</p>
          <p class="video-subtitle">时长：{{ formatDuration(selectedVideo.duration) }}</p>
          <p class="video-description">{{ selectedVideo.description || '暂无简介' }}</p>
          <div class="drawer-actions">
            <button class="approve-button" type="button" @click="reviewVideo(0)">通过</button>
            <button class="reject-button" type="button" @click="reviewVideo(1)">拒绝</button>
          </div>
        </template>
        <template v-else>
          <h2>审核抽屉占位</h2>
          <p>这里后续展示封面、标题、作者、简介和通过/拒绝操作。</p>
        </template>
      </aside>
    </div>
  </section>
</template>
