<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import CommentList from '../components/CommentList.vue'
import { authState } from '../lib/auth'
import { api } from '../lib/api'
import { formatCount, formatDateTime } from '../lib/format'
import type { CommentVO, VideoDetailVO } from '../types'

const route = useRoute()

const detail = ref<VideoDetailVO | null>(null)
const comments = ref<CommentVO[]>([])
const loading = ref(true)
const actionLoading = ref(false)
const error = ref('')
const commentError = ref('')
const replyTarget = ref<CommentVO | null>(null)
const commentForm = reactive({
  content: '',
})

const videoId = computed(() => String(route.params.id ?? '').trim())

async function loadVideo() {
  if (!/^\d+$/.test(videoId.value)) {
    error.value = '视频参数无效'
    return
  }

  loading.value = true
  error.value = ''
  commentError.value = ''
  try {
    const [video, commentList] = await Promise.all([
      api.get<VideoDetailVO>(`/videos/${videoId.value}`),
      api.get<CommentVO[]>(`/videos/${videoId.value}/comments`, {
        pageNo: 1,
        pageSize: 20,
      }),
    ])
    detail.value = video
    comments.value = commentList
    api.post<void>(`/videos/${videoId.value}/views`).catch(() => undefined)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载视频失败'
  } finally {
    loading.value = false
  }
}

async function toggleVideoLike() {
  if (!detail.value || !authState.token) {
    error.value = '请先登录'
    return
  }
  actionLoading.value = true
  try {
    if (detail.value.isLiked) {
      await api.delete<void>(`/me/videos/${detail.value.id}/likes`)
      detail.value.isLiked = false
      detail.value.likeCount = Math.max(0, detail.value.likeCount - 1)
    } else {
      await api.post<void>(`/me/videos/${detail.value.id}/likes`)
      detail.value.isLiked = true
      detail.value.likeCount += 1
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '更新点赞失败'
  } finally {
    actionLoading.value = false
  }
}

async function toggleFollow() {
  if (!detail.value || !authState.token) {
    error.value = '请先登录'
    return
  }
  actionLoading.value = true
  try {
    const authorUid = detail.value.author.uid
    if (detail.value.isFollowed) {
      await api.delete<void>(`/me/followings/${authorUid}`)
      detail.value.isFollowed = false
    } else {
      await api.post<void>(`/me/followings/${authorUid}`)
      detail.value.isFollowed = true
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '更新关注失败'
  } finally {
    actionLoading.value = false
  }
}

async function reloadComments() {
  comments.value = await api.get<CommentVO[]>(`/videos/${videoId.value}/comments`, {
    pageNo: 1,
    pageSize: 20,
  })
}

async function submitComment() {
  if (!authState.token) {
    commentError.value = '请先登录'
    return
  }
  if (!commentForm.content.trim()) {
    commentError.value = '评论内容不能为空'
    return
  }
  commentError.value = ''
  try {
    await api.post<number>(`/me/videos/${videoId.value}/comments`, {
      content: commentForm.content.trim(),
      parentId: replyTarget.value?.id || 0,
    })
    commentForm.content = ''
    replyTarget.value = null
    await reloadComments()
    if (detail.value) {
      detail.value.commentCount += 1
    }
  } catch (err) {
    commentError.value = err instanceof Error ? err.message : '发表评论失败'
  }
}

async function toggleCommentLike(comment: CommentVO) {
  if (!authState.token) {
    commentError.value = '请先登录'
    return
  }
  try {
    if (comment.isLiked) {
      await api.delete<void>(`/me/comments/${comment.id}/likes`)
      comment.isLiked = false
      comment.likeCount = Math.max(0, comment.likeCount - 1)
    } else {
      await api.post<void>(`/me/comments/${comment.id}/likes`)
      comment.isLiked = true
      comment.likeCount += 1
    }
  } catch (err) {
    commentError.value = err instanceof Error ? err.message : '评论点赞失败'
  }
}

async function deleteComment(comment: CommentVO) {
  if (!authState.token) {
    return
  }
  try {
    await api.delete<void>(`/me/comments/${comment.id}`)
    await reloadComments()
    if (detail.value) {
      detail.value.commentCount = Math.max(0, detail.value.commentCount - 1)
    }
  } catch (err) {
    commentError.value = err instanceof Error ? err.message : '删除评论失败'
  }
}

watch(() => route.params.id, loadVideo, { immediate: true })
onMounted(loadVideo)
</script>

<template>
  <section v-if="loading" class="loading-state">正在加载视频详情…</section>
  <section v-else-if="error" class="error-state">{{ error }}</section>
  <section v-else-if="detail" class="video-layout">
    <div class="video-main">
      <article class="player-card panel">
        <video class="player" :src="detail.videoUrl" :poster="detail.coverUrl" controls playsinline />
      </article>

      <article class="video-panel panel">
        <h1>{{ detail.title }}</h1>
        <div class="status-line">
          <span>{{ formatCount(detail.viewCount) }} 播放</span>
          <span>{{ formatCount(detail.likeCount) }} 点赞</span>
          <span>{{ formatCount(detail.commentCount) }} 评论</span>
          <span>{{ formatDateTime(detail.uploadDate) }}</span>
        </div>
        <p class="video-desc">{{ detail.desc || '暂无简介。' }}</p>
        <div class="tag-row">
          <span v-for="tag in detail.tags" :key="tag" class="tag-chip">{{ tag }}</span>
        </div>
        <div class="video-actions">
          <button class="primary-button" :disabled="actionLoading" @click="toggleVideoLike">
            {{ detail.isLiked ? '取消点赞' : '点赞视频' }}
          </button>
          <button class="secondary-button" :disabled="actionLoading" @click="toggleFollow">
            {{ detail.isFollowed ? '取消关注作者' : '关注作者' }}
          </button>
        </div>
      </article>

      <article class="comment-panel panel">
        <div class="section-title">
          <h2>评论区</h2>
        </div>
        <div class="field-grid">
          <div class="field-group">
            <label for="comment-content">
              {{ replyTarget ? `回复 ${replyTarget.nickname}` : '发表评论' }}
            </label>
            <textarea id="comment-content" v-model="commentForm.content" placeholder="输入评论内容" />
          </div>
          <div class="status-line">
            <button class="primary-button" @click="submitComment">提交评论</button>
            <button v-if="replyTarget" class="secondary-button" @click="replyTarget = null">取消回复</button>
          </div>
          <p v-if="commentError" class="error-state">{{ commentError }}</p>
        </div>
        <div class="comment-list-wrap">
          <CommentList
            :comments="comments"
            :current-uid="authState.uid"
            @reply="replyTarget = $event"
            @toggle-like="toggleCommentLike"
            @delete="deleteComment"
          />
        </div>
      </article>
    </div>

    <aside class="video-side">
      <article class="author-card panel">
        <div class="author-head">
          <img :src="detail.author.avatar" :alt="detail.author.nickname" />
          <div>
            <RouterLink :to="`/user/${detail.author.uid}`">
              <strong>{{ detail.author.nickname }}</strong>
            </RouterLink>
            <p>{{ detail.author.sign || '这个作者还没有签名。' }}</p>
          </div>
        </div>
      </article>
    </aside>
  </section>
</template>

<style scoped>
.video-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 24px;
}

.video-main {
  display: grid;
  gap: 20px;
}

.player-card {
  overflow: hidden;
}

.player {
  width: 100%;
  display: block;
  background: #000;
  aspect-ratio: 16 / 9;
}

.video-panel,
.comment-panel,
.author-card {
  padding: 24px;
}

.video-panel h1 {
  margin: 0 0 12px;
  font-family: var(--font-heading);
}

.video-desc {
  margin: 18px 0;
  white-space: pre-wrap;
}

.tag-row,
.video-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.comment-list-wrap {
  margin-top: 20px;
}

.author-head {
  display: flex;
  gap: 14px;
  align-items: center;
}

.author-head img {
  width: 68px;
  height: 68px;
  border-radius: 50%;
  object-fit: cover;
}

.author-head p {
  margin: 6px 0 0;
  color: var(--muted);
}

@media (max-width: 1080px) {
  .video-layout {
    grid-template-columns: 1fr;
  }
}
</style>
