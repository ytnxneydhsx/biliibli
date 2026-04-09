<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import VideoCard from '../components/VideoCard.vue'
import { formatCount, formatDate } from '../lib/format'
import { api } from '../lib/api'
import type { PageVO, VideoRankVO, VideoVO } from '../types'

const videos = ref<VideoVO[]>([])
const ranks = ref<VideoRankVO[]>([])
const loading = ref(true)
const error = ref('')

const featuredVideo = computed(() => videos.value[0] ?? null)
const feedVideos = computed(() => videos.value.slice(1))
const featuredSummary = computed(() => {
  if (!featuredVideo.value) {
    return ''
  }

  return `${featuredVideo.value.nickname} 刚刚发布的公开视频，已经可以直接进入详情页观看。`
})

function formatRank(rank: number) {
  return String(rank).padStart(2, '0')
}

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
  <section class="home-page">
    <section class="hero-grid">
      <article v-if="featuredVideo" class="featured-hero panel">
        <div class="featured-copy">
          <span class="tag-chip">今日推荐</span>
          <h1>{{ featuredVideo.title }}</h1>
          <p>{{ featuredSummary }}</p>
          <div class="status-line featured-stats">
            <span>{{ formatCount(featuredVideo.viewCount) }} 播放</span>
            <span>{{ formatDate(featuredVideo.createTime) }}</span>
            <span>UP 主 {{ featuredVideo.nickname }}</span>
          </div>
          <div class="hero-actions">
            <RouterLink
              class="primary-button"
              :to="`/video/${featuredVideo.id}`"
              data-testid="featured-watch-link"
            >
              立即观看
            </RouterLink>
            <RouterLink class="secondary-button" :to="{ name: 'search' }">去搜索</RouterLink>
            <RouterLink class="secondary-button" to="/studio">创作中心</RouterLink>
          </div>
        </div>

        <RouterLink class="featured-cover" :to="`/video/${featuredVideo.id}`">
          <img :src="featuredVideo.coverUrl" :alt="featuredVideo.title" />
        </RouterLink>
      </article>

      <article v-else-if="!loading && !error" class="panel empty-state">
        当前还没有可展示的视频。
      </article>

      <article v-else-if="error" class="panel error-state">
        {{ error }}
      </article>

      <article v-else class="panel loading-state">
        正在准备首页主推荐…
      </article>

      <aside class="hero-rank panel">
        <div class="section-title">
          <h2>今日热榜</h2>
          <RouterLink class="text-button" :to="{ name: 'search' }">更多内容</RouterLink>
        </div>
        <div v-if="loading" class="loading-state">正在加载排行榜…</div>
        <div v-else-if="error" class="error-state">{{ error }}</div>
        <div v-else-if="ranks.length" class="rank-list">
          <RouterLink
            v-for="item in ranks"
            :key="item.id"
            class="hero-rank-item"
            :to="`/video/${item.id}`"
          >
            <span class="hero-rank-number">{{ formatRank(item.rank) }}</span>
            <img :src="item.coverUrl" :alt="item.title" />
            <div class="hero-rank-copy">
              <strong>{{ item.title }}</strong>
              <span>{{ item.nickname }}</span>
              <span>{{ formatCount(item.viewCount) }} 播放</span>
            </div>
          </RouterLink>
        </div>
        <div v-else class="empty-state">排行榜暂时还没有内容。</div>
      </aside>
    </section>

    <section class="content-section">
      <div class="section-title">
        <div class="content-heading">
          <h2>最新公开视频</h2>
          <p class="muted">看看最近更新了哪些值得点开的内容。</p>
        </div>
        <RouterLink class="text-button" :to="{ name: 'search' }">去搜索</RouterLink>
      </div>

      <div v-if="loading" class="loading-state">正在加载视频…</div>
      <div v-else-if="error" class="error-state">{{ error }}</div>
      <div v-else-if="feedVideos.length" class="video-grid">
        <VideoCard v-for="video in feedVideos" :key="video.id" :video="video" />
      </div>
      <div v-else class="empty-state">首页主推荐位之外还没有更多公开视频。</div>
    </section>
  </section>
</template>

<style scoped>
.home-page {
  display: grid;
  gap: 30px;
}

.hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(300px, 0.88fr);
  gap: 24px;
}

.featured-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(300px, 0.92fr);
  gap: 22px;
  padding: 26px;
  overflow: hidden;
}

.featured-hero::before {
  content: "";
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at top left, rgba(251, 114, 153, 0.18), transparent 34%),
    radial-gradient(circle at bottom right, rgba(0, 161, 214, 0.18), transparent 36%);
  pointer-events: none;
}

.featured-copy,
.featured-cover,
.hero-rank {
  position: relative;
  z-index: 1;
}

.featured-copy {
  display: grid;
  align-content: center;
}

.featured-copy h1 {
  margin: 18px 0 14px;
  font-family: var(--font-heading);
  font-size: clamp(34px, 5vw, 60px);
  line-height: 1;
}

.featured-copy p {
  max-width: 50ch;
  margin: 0;
  color: var(--muted);
}

.featured-stats {
  margin-top: 18px;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 24px;
}

.featured-cover {
  min-height: 320px;
  border-radius: 24px;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(251, 114, 153, 0.16), rgba(0, 161, 214, 0.2));
  box-shadow: 0 18px 32px rgba(31, 36, 51, 0.12);
}

.featured-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-rank {
  padding: 24px;
}

.rank-list {
  display: grid;
  gap: 12px;
}

.hero-rank-item {
  display: grid;
  grid-template-columns: 48px 92px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  padding: 10px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(103, 112, 131, 0.12);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.hero-rank-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 28px rgba(31, 36, 51, 0.08);
}

.hero-rank-number {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(251, 114, 153, 0.2), rgba(255, 170, 110, 0.22));
  color: var(--pink-deep);
  font-family: var(--font-heading);
  font-size: 20px;
  font-weight: 800;
}

.hero-rank-item img {
  width: 92px;
  height: 64px;
  border-radius: 14px;
  object-fit: cover;
}

.hero-rank-copy {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.hero-rank-copy strong {
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.hero-rank-copy span {
  color: var(--muted);
  font-size: 13px;
}

.content-section {
  display: grid;
  gap: 18px;
}

.content-heading {
  display: grid;
  gap: 6px;
}

.content-heading p {
  margin: 0;
}

.video-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

@media (max-width: 1200px) {
  .hero-grid {
    grid-template-columns: 1fr;
  }

  .featured-hero {
    grid-template-columns: 1fr;
  }

  .video-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .featured-hero,
  .hero-rank {
    padding: 20px;
  }

  .featured-cover {
    min-height: 220px;
  }

  .video-grid {
    grid-template-columns: 1fr;
  }
}
</style>
