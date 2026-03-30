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
  padding: 22px;
}

.messages-debug summary {
  cursor: pointer;
  font-weight: 700;
}

.debug-list {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.debug-item {
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.72);
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
