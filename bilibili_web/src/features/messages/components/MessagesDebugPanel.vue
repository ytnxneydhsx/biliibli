<script setup lang="ts">
import type { EventLogItem } from '../types'

defineProps<{
  eventLogs: EventLogItem[]
}>()
</script>

<template>
  <details class="messages-debug panel">
    <summary>调试事件</summary>
    <div class="debug-list">
      <article v-for="item in eventLogs" :key="`${item.time}-${item.type}-${item.body}`" class="debug-item">
        <div class="debug-top">
          <strong>{{ item.type }}</strong>
          <span>{{ item.time }}</span>
        </div>
        <pre>{{ item.body }}</pre>
      </article>
    </div>
  </details>
</template>

<style scoped>
.messages-debug {
  margin: 10px 12px 14px;
  padding: 12px 14px;
  border: 1px solid var(--bili-line, #e3e5e7);
  border-radius: 8px;
  background: rgba(18, 29, 43, 0.82);
  color: var(--bili-muted, #8ea1b8);
  box-shadow: none;
  backdrop-filter: none;
}

.messages-debug summary {
  cursor: pointer;
  font-weight: 700;
}

.debug-list {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.debug-item {
  padding: 12px 14px;
  border-radius: 8px;
  border: 1px solid var(--bili-line, #e3e5e7);
  background: rgba(255, 255, 255, 0.05);
}

.debug-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.debug-item pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 720px) {
  .messages-debug {
    padding: 16px;
  }
}
</style>
