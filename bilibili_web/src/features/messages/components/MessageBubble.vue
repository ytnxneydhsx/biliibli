<script setup lang="ts">
import type { MessageItem } from '../types'

defineProps<{
  item: MessageItem
  peerName: string
}>()
</script>

<template>
  <article class="message-bubble" :class="[item.direction, { pending: item.pending, failed: item.failed }]">
    <div class="message-meta">
      <span>{{ item.direction === 'outgoing' ? '我' : peerName }}</span>
      <span>{{ item.time }}</span>
    </div>
    <p v-if="item.text">{{ item.text }}</p>
    <div v-if="item.imageUrls.length" class="message-images">
      <img
        v-for="url in item.imageUrls"
        :key="url"
        class="message-image"
        :src="url"
        alt="聊天图片"
      />
    </div>
    <div v-if="item.senderLocation" class="message-location">
      {{ item.senderLocation }}
    </div>
    <div v-if="item.failed" class="message-fail-reason">
      {{ item.failReason }}
    </div>
  </article>
</template>

<style scoped>
.message-bubble {
  max-width: min(72%, 520px);
  padding: 12px 14px;
  border-radius: 16px;
  border: 1px solid var(--bili-line, #e3e5e7);
  color: var(--bili-text, #f7fbff);
  box-shadow: none;
}

.message-bubble.incoming {
  align-self: flex-start;
  background: rgba(255, 255, 255, 0.07);
}

.message-bubble.outgoing {
  align-self: flex-end;
  background: linear-gradient(135deg, rgba(50, 197, 255, 0.16), rgba(251, 114, 153, 0.12));
  border-color: rgba(50, 197, 255, 0.22);
}

.message-bubble.pending {
  opacity: 0.72;
}

.message-bubble.failed {
  opacity: 1;
  border-color: rgba(255, 109, 138, 0.42);
  background: rgba(255, 109, 138, 0.1);
}

.message-fail-reason {
  margin-top: 8px;
  color: var(--bili-pink, #fb7299);
  font-size: 12px;
}

.message-meta {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 8px;
  color: var(--bili-subtle, #9499a0);
  font-size: 12px;
}

.message-bubble p {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.65;
  font-size: 14px;
}

.message-images {
  display: grid;
  gap: 8px;
  margin-top: 10px;
}

.message-location {
  margin-top: 10px;
  color: var(--bili-subtle, #9499a0);
  font-size: 12px;
}

.message-image {
  width: min(280px, 100%);
  border-radius: 12px;
  border: 1px solid var(--bili-line, #e3e5e7);
  object-fit: cover;
}

@media (max-width: 720px) {
  .message-bubble {
    max-width: 88%;
  }
}
</style>
