<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../lib/api'
import type { AdminVideoVO, CursorPageVO } from '../types'

type VideoTabKey = 'pending' | 'published' | 'deleted'

const TAB_CONFIG: Record<VideoTabKey, { label: string; endpoint: string; statusLabel: string; summary: string }> = {
  pending: {
    label: '待审核',
    endpoint: '/admin/videos/pending',
    statusLabel: '待审核',
    summary: '当前展示待审核视频',
  },
  published: {
    label: '已通过',
    endpoint: '/admin/videos/published',
    statusLabel: '已上架',
    summary: '当前展示已上架视频',
  },
  deleted: {
    label: '已拒绝',
    endpoint: '/admin/videos/deleted',
    statusLabel: '已拒绝',
    summary: '当前展示已拒绝视频',
  },
}

const videos = ref<AdminVideoVO[]>([])
const loading = ref(false)
const errorMessage = ref('')
const nextCursor = ref<string | null>(null)
const hasMore = ref(false)
const selectedVideoId = ref<string | null>(null)
const activeTab = ref<VideoTabKey>('pending')

const selectedVideo = computed(() =>
  videos.value.find((video) => video.id === selectedVideoId.value) ?? null,
)

const activeTabConfig = computed(() => TAB_CONFIG[activeTab.value])
const emptyTitle = computed(() => (loading.value ? `正在加载${activeTabConfig.value.label}视频…` : `当前没有${activeTabConfig.value.label}视频`))
const emptyDescription = computed(() =>
  loading.value ? '请稍候，管理员列表正在从后端拉取数据。' : `${activeTabConfig.value.summary}。`,
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

async function loadVideos() {
  loading.value = true
  errorMessage.value = ''

  try {
    const result = await api.get<CursorPageVO<AdminVideoVO>>(activeTabConfig.value.endpoint)
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

async function switchTab(tab: VideoTabKey) {
  if (tab === activeTab.value) {
    return
  }

  activeTab.value = tab
  selectedVideoId.value = null
  await loadVideos()
}

async function reviewVideo(status: 0 | 1) {
  if (!selectedVideo.value || activeTab.value !== 'pending') {
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    await api.put(`/admin/videos/${selectedVideo.value.id}/status`, { status })
    await loadVideos()
  } catch (error) {
    errorMessage.value = (error as Error).message
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadVideos()
})
</script>

<template>
  <section class="page-section">
    <header class="page-header">
      <div>
        <h1>视频管理</h1>
        <p>管理员后台默认首页，支持查看待审核、已上架和已拒绝视频。</p>
      </div>
    </header>

    <div class="tabs-row">
      <button
        v-for="(tab, key) in TAB_CONFIG"
        :key="key"
        class="tab-button"
        :class="{ 'is-active': activeTab === key }"
        :data-tab="key"
        type="button"
        @click="switchTab(key as VideoTabKey)"
      >
        {{ tab.label }}
      </button>
    </div>

    <div class="video-layout">
      <section class="panel">
        <div class="panel-header">
          <div>
            <h2>视频列表</h2>
            <p>封面 / 标题 / 投稿人 / 投稿时间 / 时长 / 当前状态 / 操作按钮</p>
            <p class="hint-text">{{ activeTabConfig.summary }}</p>
          </div>
          <button class="refresh-button" type="button" @click="loadVideos">刷新</button>
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
                <span class="status-badge">{{ activeTabConfig.statusLabel }}</span>
              </div>
              <p class="video-subtitle">投稿人：{{ video.nickname }} · 投稿时间：{{ video.createTime }}</p>
              <p class="video-subtitle">时长：{{ formatDuration(video.duration) }} · 作者ID：{{ video.authorUid }}</p>
              <p class="video-description">{{ video.description || '暂无简介' }}</p>
              <button class="review-link" type="button" @click="openVideoReview(video.id)">查看审核</button>
            </div>
          </li>
        </ul>

        <p v-if="hasMore" class="hint-text">还有更多{{ activeTabConfig.label }}视频，后续可接入下一页。</p>
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
          <div v-if="activeTab === 'pending'" class="drawer-actions">
            <button class="approve-button" type="button" @click="reviewVideo(0)">通过</button>
            <button class="reject-button" type="button" @click="reviewVideo(1)">拒绝</button>
          </div>
          <p v-else class="hint-text">当前视频状态：{{ activeTabConfig.statusLabel }}</p>
        </template>
        <template v-else>
          <h2>审核抽屉占位</h2>
          <p>这里后续展示封面、标题、作者、简介和通过/拒绝操作。</p>
        </template>
      </aside>
    </div>
  </section>
</template>
