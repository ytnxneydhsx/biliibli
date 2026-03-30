<script setup lang="ts">
import type { MessageItem } from '../types'

defineProps<{
  item: MessageItem
  peerName: string
}>()
</script>

<template>
  <article class="message-bubble" :class="[item.direction, { pending: item.pending }]">
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
  </article>
</template>

<style scoped>
.message-bubble {
  max-width: min(72%, 560px);
  padding: 14px 16px;
  border-radius: 20px;
  border: 1px solid transparent;
}

.message-bubble.incoming {
  align-self: flex-start;
  background: #fff;
  border-color: var(--line);
}

.message-bubble.outgoing {
  align-self: flex-end;
  background: linear-gradient(135deg, rgba(0, 161, 214, 0.12), rgba(251, 114, 153, 0.1));
  border-color: rgba(0, 161, 214, 0.18);
}

.message-bubble.pending {
  opacity: 0.72;
}

.message-meta {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 8px;
  color: var(--muted);
  font-size: 12px;
}

.message-bubble p {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
}

.message-images {
  display: grid;
  gap: 8px;
  margin-top: 10px;
}

.message-location {
  margin-top: 10px;
  color: var(--muted);
  font-size: 12px;
}

.message-image {
  width: min(280px, 100%);
  border-radius: 16px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  object-fit: cover;
}

@media (max-width: 720px) {
  .message-bubble {
    max-width: 88%;
  }
}
</style>
