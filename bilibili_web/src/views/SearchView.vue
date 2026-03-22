<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import UserCard from '../components/UserCard.vue'
import VideoCard from '../components/VideoCard.vue'
import { authState } from '../lib/auth'
import { api } from '../lib/api'
import type { PageVO, UserSearchVO, VideoVO } from '../types'

const route = useRoute()
const router = useRouter()

const keyword = ref('')
const tab = ref<'videos' | 'users'>('videos')
const loading = ref(false)
const error = ref('')
const videoResults = ref<VideoVO[]>([])
const userResults = ref<PageVO<UserSearchVO> | null>(null)
const history = ref<string[]>([])

async function loadHistory() {
  if (!authState.token) {
    history.value = []
    return
  }
  try {
    history.value = await api.get<string[]>('/search/videos/history')
  } catch {
    history.value = []
  }
}

async function runSearch() {
  const q = keyword.value.trim()
  if (!q) {
    videoResults.value = []
    userResults.value = null
    error.value = ''
    return
  }

  loading.value = true
  error.value = ''
  try {
    if (tab.value === 'videos') {
      videoResults.value = await api.get<VideoVO[]>('/search/videos', {
        keyword: q,
        pageNo: 1,
        pageSize: 20,
      })
      userResults.value = null
    } else {
      userResults.value = await api.get<PageVO<UserSearchVO>>('/search/users', {
        nickname: q,
        timeOrder: 'desc',
        pageNo: 1,
        pageSize: 20,
      })
      videoResults.value = []
    }
    await loadHistory()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '搜索失败'
  } finally {
    loading.value = false
  }
}

function applyQuery() {
  const q = keyword.value.trim()
  router.push({
    name: 'search',
    query: {
      ...(q ? { q } : {}),
      tab: tab.value,
    },
  })
}

watch(
  () => [route.query.q, route.query.tab],
  async ([q, activeTab]) => {
    keyword.value = typeof q === 'string' ? q : ''
    tab.value = activeTab === 'users' ? 'users' : 'videos'
    await runSearch()
  },
  { immediate: true },
)

onMounted(loadHistory)
</script>

<template>
  <section class="search-layout">
    <article class="search-main panel">
      <div class="search-toolbar">
        <form class="search-inline" @submit.prevent="applyQuery">
          <input v-model.trim="keyword" type="search" placeholder="输入关键词搜索" />
          <button class="primary-button" type="submit">搜索</button>
        </form>
        <div class="auth-tabs">
          <button
            :class="tab === 'videos' ? 'primary-button' : 'secondary-button'"
            type="button"
            @click="tab = 'videos'; applyQuery()"
          >
            视频
          </button>
          <button
            :class="tab === 'users' ? 'primary-button' : 'secondary-button'"
            type="button"
            @click="tab = 'users'; applyQuery()"
          >
            用户
          </button>
        </div>
      </div>

      <div v-if="loading" class="loading-state">正在搜索…</div>
      <div v-else-if="error" class="error-state">{{ error }}</div>
      <div v-else-if="tab === 'videos'">
        <div v-if="videoResults.length" class="video-grid">
          <VideoCard v-for="video in videoResults" :key="video.id" :video="video" />
        </div>
        <div v-else class="empty-state">请输入关键词后搜索视频。</div>
      </div>
      <div v-else>
        <div v-if="userResults?.records.length" class="user-grid">
          <UserCard v-for="user in userResults.records" :key="user.uid" :user="user" />
        </div>
        <div v-else class="empty-state">请输入关键词后搜索用户。</div>
      </div>
    </article>

    <aside class="search-side panel">
      <div class="section-title">
        <h3>搜索历史</h3>
      </div>
      <div v-if="history.length" class="history-list">
        <button
          v-for="item in history"
          :key="item"
          class="pill history-chip"
          type="button"
          @click="keyword = item; tab = 'videos'; applyQuery()"
        >
          {{ item }}
        </button>
      </div>
      <div v-else class="empty-state">当前没有历史记录。</div>
    </aside>
  </section>
</template>

<style scoped>
.search-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 24px;
}

.search-main,
.search-side {
  padding: 24px;
}

.search-toolbar {
  display: grid;
  gap: 18px;
  margin-bottom: 24px;
}

.search-inline {
  display: flex;
  gap: 12px;
}

.search-inline input {
  flex: 1;
  min-width: 0;
  border: 1px solid var(--line);
  border-radius: 999px;
  padding: 13px 16px;
}

.auth-tabs {
  display: flex;
  gap: 12px;
}

.video-grid,
.user-grid {
  display: grid;
  gap: 18px;
}

.video-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.history-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.history-chip {
  background: #fff;
}

@media (max-width: 1100px) {
  .search-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .video-grid {
    grid-template-columns: 1fr;
  }

  .search-inline {
    flex-direction: column;
  }
}
</style>
