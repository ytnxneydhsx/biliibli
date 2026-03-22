<script setup lang="ts">
import { onMounted, ref } from 'vue'
import VideoCard from '../components/VideoCard.vue'
import { api } from '../lib/api'
import type { PageVO, VideoRankVO, VideoVO } from '../types'

const videos = ref<VideoVO[]>([])
const ranks = ref<VideoRankVO[]>([])
const loading = ref(true)
const error = ref('')

async function loadHome() {
  loading.value = true
  error.value = ''
  try {
    const [videoPage, rankPage] = await Promise.all([
      api.get<PageVO<VideoVO>>('/videos', { pageNo: 1, pageSize: 12 }),
      api.get<PageVO<VideoRankVO>>('/videos/rank', { pageNo: 1, pageSize: 8 }),
    ])
    videos.value = videoPage.records
    ranks.value = rankPage.records
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载首页失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadHome)
</script>

<template>
  <section class="hero-grid">
    <div class="hero-card panel">
      <span class="tag-chip">仿 B 站首屏</span>
      <h1>只呈现后端已存在的能力，不做假按钮。</h1>
      <p>
        首版页面覆盖登录注册、公开视频流、排行榜、搜索、视频详情、评论互动、关注关系、个人资料和上传中心。
      </p>
      <div class="hero-actions">
        <RouterLink class="primary-button" to="/studio">进入创作中心</RouterLink>
        <RouterLink class="secondary-button" :to="{ name: 'search' }">查看搜索页</RouterLink>
      </div>
    </div>

    <aside class="hero-rank panel">
      <div class="section-title">
        <h2>热视频榜</h2>
      </div>
      <div v-if="loading" class="loading-state">正在加载排行榜…</div>
      <div v-else-if="error" class="error-state">{{ error }}</div>
      <div v-else class="rank-list">
        <VideoCard v-for="item in ranks" :key="item.id" :video="item" compact />
      </div>
    </aside>
  </section>

  <section class="content-section">
    <div class="section-title">
      <h2>最新公开视频</h2>
      <RouterLink class="text-button" :to="{ name: 'search' }">去搜索</RouterLink>
    </div>
    <div v-if="loading" class="loading-state">正在加载视频…</div>
    <div v-else-if="error" class="error-state">{{ error }}</div>
    <div v-else class="video-grid">
      <VideoCard v-for="video in videos" :key="video.id" :video="video" />
    </div>
  </section>
</template>

<style scoped>
.hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(320px, 0.9fr);
  gap: 24px;
}

.hero-card {
  padding: 34px;
  overflow: hidden;
  position: relative;
}

.hero-card::after {
  content: "";
  position: absolute;
  inset: auto -80px -100px auto;
  width: 280px;
  height: 280px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(251, 114, 153, 0.24), transparent 68%);
}

.hero-card h1 {
  margin: 18px 0 14px;
  max-width: 12ch;
  font-family: var(--font-heading);
  font-size: clamp(34px, 5vw, 58px);
  line-height: 1.02;
}

.hero-card p {
  max-width: 52ch;
  color: var(--muted);
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 26px;
}

.hero-rank {
  padding: 24px;
}

.rank-list {
  display: grid;
  gap: 14px;
}

.content-section {
  margin-top: 30px;
}

.video-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
}

@media (max-width: 1200px) {
  .video-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .hero-grid {
    grid-template-columns: 1fr;
  }

  .hero-card {
    padding: 22px;
  }

  .video-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .video-grid {
    grid-template-columns: 1fr;
  }
}
</style>
