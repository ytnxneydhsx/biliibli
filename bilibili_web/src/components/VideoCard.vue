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
  background: rgba(18, 30, 45, 0.82);
  border-color: rgba(165, 188, 218, 0.1);
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
}

.video-card:hover {
  transform: translateY(-3px);
  border-color: rgba(251, 114, 153, 0.28);
  box-shadow: 0 18px 42px rgba(0, 0, 0, 0.32);
}

.video-cover {
  position: relative;
  display: block;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(251, 114, 153, 0.14), rgba(50, 197, 255, 0.12));
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
  transition: transform 0.35s ease;
}

.video-card:hover .video-cover img {
  transform: scale(1.035);
}

.video-duration,
.video-rank {
  position: absolute;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  color: #fff;
  font-size: 12px;
  font-weight: 800;
}

.video-duration {
  right: 10px;
  bottom: 10px;
  background: rgba(7, 12, 20, 0.76);
  backdrop-filter: blur(10px);
}

.video-rank {
  top: 10px;
  left: 10px;
  background: linear-gradient(135deg, var(--pink) 0%, #ffbd5a 100%);
}

.video-body {
  display: grid;
  gap: 7px;
  padding: 12px 12px 13px;
}

.video-title {
  display: -webkit-box;
  min-height: 41px;
  overflow: hidden;
  font-weight: 700;
  line-height: 1.4;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.video-author {
  color: #b9c7d6;
  font-size: 14px;
}

.video-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  color: #8294a8;
  font-size: 13px;
}

.compact .video-body {
  padding: 12px;
}

.compact .video-title {
  min-height: auto;
}
</style>
