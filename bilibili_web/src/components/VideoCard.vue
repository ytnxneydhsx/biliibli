<script setup lang="ts">
import { computed } from 'vue'
import type { VideoRankVO, VideoVO } from '../types'
import { formatCount, formatDate, formatDuration } from '../lib/format'

const props = defineProps<{
  video: VideoVO | VideoRankVO
  compact?: boolean
}>()

const href = computed(() => `/video/${props.video.id}`)
const authorHref = computed(() => `/user/${props.video.authorUid}`)
</script>

<template>
  <article class="video-card panel" :class="{ compact }">
    <RouterLink class="video-cover" :to="href">
      <img :src="video.coverUrl" :alt="video.title" />
      <span class="video-duration">{{ formatDuration(video.duration) }}</span>
      <span v-if="'rank' in video" class="video-rank">TOP {{ video.rank }}</span>
    </RouterLink>

    <div class="video-body">
      <RouterLink class="video-title" :to="href">{{ video.title }}</RouterLink>
      <RouterLink class="video-author" :to="authorHref">{{ video.nickname }}</RouterLink>
      <div class="video-meta">
        <span>{{ formatCount(video.viewCount) }} 播放</span>
        <span>{{ formatDate(video.createTime) }}</span>
      </div>
    </div>
  </article>
</template>

<style scoped>
.video-card {
  position: relative;
  overflow: hidden;
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
}

.video-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 18px 36px rgba(52, 80, 130, 0.16);
}

.video-cover {
  position: relative;
  display: block;
  aspect-ratio: 16 / 10;
  background: linear-gradient(135deg, rgba(251, 114, 153, 0.16), rgba(0, 161, 214, 0.18));
}

.video-cover::after {
  content: "";
  position: absolute;
  inset: auto 0 0;
  height: 48%;
  background: linear-gradient(180deg, transparent, rgba(18, 28, 45, 0.46));
}

.video-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-duration,
.video-rank {
  position: absolute;
  padding: 4px 8px;
  border-radius: 999px;
  color: #fff;
  font-size: 12px;
}

.video-duration {
  right: 10px;
  bottom: 10px;
  background: rgba(15, 23, 42, 0.72);
}

.video-rank {
  top: 10px;
  left: 10px;
  background: linear-gradient(135deg, var(--pink) 0%, #ff9a5c 100%);
}

.video-body {
  display: grid;
  gap: 8px;
  padding: 16px;
}

.video-title {
  display: -webkit-box;
  min-height: 44px;
  overflow: hidden;
  font-weight: 700;
  line-height: 1.4;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.video-author {
  color: var(--muted);
  font-size: 14px;
}

.video-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  color: var(--muted);
  font-size: 13px;
}

.compact .video-body {
  padding: 12px;
}

.compact .video-title {
  min-height: auto;
}
</style>
