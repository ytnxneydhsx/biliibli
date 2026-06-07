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

  return `${featuredVideo.value.nickname} 刚刚发布的公开视频，正在成为今天的推荐焦点。`
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
      <article v-if="featuredVideo" class="featured-hero">
        <RouterLink class="featured-cover" :to="`/video/${featuredVideo.id}`">
          <img :src="featuredVideo.coverUrl" :alt="featuredVideo.title" />
        </RouterLink>
        <div class="featured-overlay"></div>
        <div class="featured-copy">
          <span class="tag-chip">推荐</span>
          <h1>{{ featuredVideo.title }}</h1>
          <p>{{ featuredSummary }}</p>
          <div class="featured-author">
            <span class="avatar-dot">{{ featuredVideo.nickname.slice(0, 1) }}</span>
            <strong>{{ featuredVideo.nickname }}</strong>
            <span>{{ formatCount(featuredVideo.viewCount) }} 播放</span>
            <span>{{ formatDate(featuredVideo.createTime) }}</span>
          </div>
          <div class="hero-actions">
            <RouterLink
              class="primary-button"
              :to="`/video/${featuredVideo.id}`"
              data-testid="featured-watch-link"
            >
              立即观看
            </RouterLink>
          </div>
        </div>

        <div class="hero-pagination" aria-hidden="true">
          <span class="active"></span>
          <span></span>
          <span></span>
          <span></span>
        </div>
      </article>

      <article v-else-if="!loading && !error" class="empty-state hero-placeholder">
        当前还没有可展示的视频。
      </article>

      <article v-else-if="error" class="error-state hero-placeholder">
        {{ error }}
      </article>

      <article v-else class="loading-state hero-placeholder">
        正在准备首页主推荐…
      </article>

      <aside class="hero-rank panel">
        <div class="section-title">
          <h2>今日热榜</h2>
          <RouterLink class="chat-entry" :to="{ name: 'messages' }">进入聊天室</RouterLink>
        </div>
        <div v-if="loading" class="loading-state">正在加载排行榜…</div>
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
          <p class="muted">继续发现今天刚刚更新的内容。</p>
        </div>
      </div>

      <div v-if="loading" class="loading-state">正在加载视频…</div>
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
  gap: 24px;
}

.hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 2.38fr) minmax(360px, 0.92fr);
  gap: 18px;
}

.featured-hero {
  position: relative;
  min-height: 430px;
  overflow: hidden;
  border-radius: var(--radius-lg);
  background: #0d1724;
  border: 1px solid rgba(165, 188, 218, 0.12);
  box-shadow: 0 22px 64px rgba(0, 0, 0, 0.34);
}

.featured-cover,
.featured-overlay {
  position: absolute;
  inset: 0;
}

.featured-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.featured-overlay {
  background:
    linear-gradient(90deg, rgba(8, 14, 23, 0.72) 0%, rgba(8, 14, 23, 0.34) 44%, rgba(8, 14, 23, 0.04) 100%),
    linear-gradient(0deg, rgba(8, 14, 23, 0.58) 0%, rgba(8, 14, 23, 0.06) 55%);
  pointer-events: none;
}

.featured-copy {
  position: relative;
  z-index: 1;
  display: grid;
  align-content: center;
  width: min(610px, 72%);
  min-height: 430px;
  padding: 48px 42px;
}

.featured-copy h1 {
  margin: 18px 0 12px;
  font-family: var(--font-heading);
  font-size: clamp(42px, 4.4vw, 62px);
  line-height: 1.04;
  letter-spacing: 0;
  text-wrap: balance;
}

.featured-copy .tag-chip {
  justify-self: start;
}

.featured-copy p {
  max-width: 46ch;
  margin: 0;
  color: #b8c5d4;
}

.featured-author {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin-top: 20px;
  color: #b9c8d8;
  font-size: 14px;
}

.avatar-dot {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--pink), #ff9fbc);
  color: #fff;
  font-weight: 800;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 26px;
}

.hero-pagination {
  position: absolute;
  z-index: 1;
  left: 50%;
  bottom: 18px;
  display: flex;
  gap: 8px;
  transform: translateX(-50%);
}

.hero-pagination span {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.36);
}

.hero-pagination .active {
  width: 22px;
  background: var(--pink);
}

.hero-placeholder {
  min-height: 410px;
  display: grid;
  place-items: center;
}

.hero-rank {
  padding: 22px 18px;
  background:
    linear-gradient(180deg, rgba(21, 35, 53, 0.94), rgba(12, 23, 37, 0.94));
}

.rank-list {
  display: grid;
  gap: 9px;
}

.hero-rank-item {
  display: grid;
  grid-template-columns: 36px 106px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  min-height: 72px;
  padding: 8px;
  border-radius: var(--radius-md);
  background: transparent;
  border: 1px solid transparent;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.hero-rank-item:hover {
  transform: translateY(-2px);
  background: rgba(255, 255, 255, 0.06);
  border-color: var(--line);
  box-shadow: 0 14px 28px rgba(0, 0, 0, 0.16);
}

.hero-rank-number {
  display: grid;
  place-items: center;
  width: 34px;
  color: #778aa0;
  font-family: var(--font-heading);
  font-size: 25px;
  font-weight: 800;
}

.hero-rank-item:nth-child(-n + 3) .hero-rank-number {
  color: var(--warning);
}

.hero-rank-item img {
  width: 106px;
  height: 60px;
  border-radius: 8px;
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
  color: #f8fbff;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.hero-rank-copy span {
  color: var(--muted);
  font-size: 13px;
}

.chat-entry {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 30px;
  padding: 0 12px;
  border: 1px solid rgba(165, 188, 218, 0.18);
  border-radius: 999px;
  color: #c6d5e6;
  font-size: 13px;
  transition: background 0.2s ease, border-color 0.2s ease, color 0.2s ease;
}

.chat-entry:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(251, 114, 153, 0.36);
  color: #fff;
}

.content-section {
  display: grid;
  gap: 16px;
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
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

@media (max-width: 1200px) {
  .hero-grid {
    grid-template-columns: 1fr;
  }

  .video-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .hero-rank .section-title {
    align-items: flex-start;
    flex-direction: column;
  }

  .chat-entry {
    width: 100%;
  }

  .featured-hero,
  .featured-copy,
  .hero-placeholder {
    min-height: 360px;
  }

  .featured-copy {
    width: 100%;
    padding: 28px 22px;
  }

  .featured-copy h1 {
    font-size: 36px;
  }

  .featured-copy p {
    max-width: 32ch;
  }

  .featured-author {
    gap: 8px;
  }

  .video-grid {
    grid-template-columns: 1fr;
  }
}
</style>
