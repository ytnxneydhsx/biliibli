<script setup lang="ts">
import { computed } from 'vue'
import type { CommentVO } from '../types'
import { formatCount, formatDateTime } from '../lib/format'

const props = defineProps<{
  comments: CommentVO[]
  currentUid?: string | null
}>()

const emit = defineEmits<{
  reply: [comment: CommentVO]
  toggleLike: [comment: CommentVO]
  delete: [comment: CommentVO]
}>()

const currentUid = computed(() => props.currentUid ?? null)
</script>

<template>
  <div class="comment-stack">
    <article v-for="comment in comments" :key="comment.id" class="comment-item panel">
      <div class="comment-main">
        <img class="comment-avatar" :src="comment.avatar" :alt="comment.nickname" />
        <div class="comment-content">
          <div class="comment-topline">
            <strong>{{ comment.nickname }}</strong>
            <span>{{ formatDateTime(comment.createTime) }}</span>
          </div>
          <p>{{ comment.content }}</p>
          <div class="comment-actions">
            <button class="text-button" @click="emit('toggleLike', comment)">
              {{ comment.isLiked ? '取消赞' : '点赞' }} · {{ formatCount(comment.likeCount) }}
            </button>
            <button class="text-button" @click="emit('reply', comment)">回复</button>
            <button
              v-if="currentUid && currentUid === comment.uid"
              class="text-button danger-text"
              @click="emit('delete', comment)"
            >
              删除
            </button>
          </div>

          <div v-if="comment.childComments?.length" class="reply-stack">
            <article
              v-for="child in comment.childComments"
              :key="child.id"
              class="reply-item"
            >
              <img class="comment-avatar small" :src="child.avatar" :alt="child.nickname" />
              <div class="comment-content">
                <div class="comment-topline">
                  <strong>{{ child.nickname }}</strong>
                  <span>{{ formatDateTime(child.createTime) }}</span>
                </div>
                <p>{{ child.content }}</p>
                <div class="comment-actions">
                  <button class="text-button" @click="emit('toggleLike', child)">
                    {{ child.isLiked ? '取消赞' : '点赞' }} · {{ formatCount(child.likeCount) }}
                  </button>
                  <button
                    v-if="currentUid && currentUid === child.uid"
                    class="text-button danger-text"
                    @click="emit('delete', child)"
                  >
                    删除
                  </button>
                </div>
              </div>
            </article>
          </div>
        </div>
      </div>
    </article>
  </div>
</template>

<style scoped>
.comment-stack {
  display: grid;
  gap: 16px;
}

.comment-item {
  padding: 18px;
}

.comment-main,
.reply-item {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.comment-avatar {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  object-fit: cover;
  background: rgba(103, 112, 131, 0.12);
}

.comment-avatar.small {
  width: 36px;
  height: 36px;
}

.comment-content {
  flex: 1;
  min-width: 0;
}

.comment-topline {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 8px;
  align-items: center;
}

.comment-topline span {
  color: var(--muted);
  font-size: 13px;
}

.comment-content p {
  margin: 0;
  white-space: pre-wrap;
}

.comment-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 10px;
}

.reply-stack {
  margin-top: 14px;
  padding: 14px;
  border-radius: 16px;
  background: rgba(103, 112, 131, 0.06);
  display: grid;
  gap: 12px;
}

.danger-text {
  color: var(--danger);
}
</style>
