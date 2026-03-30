<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import UserCard from '../components/UserCard.vue'
import VideoCard from '../components/VideoCard.vue'
import { authState } from '../lib/auth'
import { api } from '../lib/api'
import type { FollowersQueryVO, PageVO, UserProfileVO, VideoVO } from '../types'

const route = useRoute()

const profile = ref<UserProfileVO | null>(null)
const videos = ref<VideoVO[]>([])
const followers = ref<FollowersQueryVO[]>([])
const followings = ref<FollowersQueryVO[]>([])
const friends = ref<FollowersQueryVO[]>([])
const error = ref('')
const loading = ref(true)
const followed = ref(false)

const uid = computed(() => String(route.params.uid ?? '').trim())
const isSelf = computed(() => authState.uid === uid.value)

async function loadFollowState() {
  if (!authState.uid || isSelf.value) {
    followed.value = false
    return
  }
  try {
    const myFollowings = await api.get<FollowersQueryVO[]>(`/users/${authState.uid}/followings`)
    followed.value = myFollowings.some((item) => item.uid === uid.value)
  } catch {
    followed.value = false
  }
}

async function loadUserSpace() {
  if (!/^\d+$/.test(uid.value)) {
    error.value = '用户参数无效'
    return
  }

  loading.value = true
  error.value = ''
  try {
    const [user, videoPage, followerList, followingList, friendList] = await Promise.all([
      api.get<UserProfileVO>(`/users/${uid.value}`),
      api.get<PageVO<VideoVO>>(`/users/${uid.value}/videos`, { pageNo: 1, pageSize: 12 }),
      api.get<FollowersQueryVO[]>(`/users/${uid.value}/followers`),
      api.get<FollowersQueryVO[]>(`/users/${uid.value}/followings`),
      api.get<FollowersQueryVO[]>(`/users/${uid.value}/friends`),
    ])

    profile.value = user
    videos.value = videoPage.records
    followers.value = followerList
    followings.value = followingList
    friends.value = friendList
    await loadFollowState()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载用户主页失败'
  } finally {
    loading.value = false
  }
}

async function toggleFollow() {
  if (!authState.token || isSelf.value) {
    return
  }
  try {
    if (followed.value) {
      await api.delete<void>(`/me/followings/${uid.value}`)
      followed.value = false
      if (profile.value) {
        profile.value.followerCount = Math.max(0, profile.value.followerCount - 1)
      }
    } else {
      await api.post<void>(`/me/followings/${uid.value}`)
      followed.value = true
      if (profile.value) {
        profile.value.followerCount += 1
      }
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '更新关注失败'
  }
}

watch(() => route.params.uid, loadUserSpace, { immediate: true })
onMounted(loadUserSpace)
</script>

<template>
  <section v-if="loading" class="loading-state">正在加载用户主页…</section>
  <section v-else-if="error" class="error-state">{{ error }}</section>
  <section v-else-if="profile" class="user-space">
    <article class="profile-hero panel">
      <img class="profile-avatar" :src="profile.avatar" :alt="profile.nickname" />
      <div class="profile-main">
        <h1>{{ profile.nickname }}</h1>
        <p>{{ profile.sign || '这个用户还没有签名。' }}</p>
        <div class="status-line">
          <span>{{ profile.followerCount }} 粉丝</span>
          <span>{{ profile.followingCount }} 关注</span>
          <span>{{ friends.length }} 互关好友</span>
        </div>
      </div>
      <div v-if="authState.token && !isSelf" class="profile-actions">
        <RouterLink class="secondary-button message-link" :to="{ name: 'messages', query: { peerUid: uid } }">私信</RouterLink>
        <button
          class="primary-button"
          type="button"
          @click="toggleFollow"
        >
          {{ followed ? '取消关注' : '关注用户' }}
        </button>
      </div>
    </article>

    <section class="user-grid-layout">
      <article class="panel block-section">
        <div class="section-title">
          <h2>公开视频</h2>
        </div>
        <div v-if="videos.length" class="video-grid">
          <VideoCard v-for="video in videos" :key="video.id" :video="video" />
        </div>
        <div v-else class="empty-state">这个用户还没有公开视频。</div>
      </article>

      <div class="side-stack">
        <article class="panel block-section">
          <div class="section-title"><h3>粉丝</h3></div>
          <div v-if="followers.length" class="user-list">
            <UserCard v-for="item in followers" :key="item.uid" :user="item" />
          </div>
          <div v-else class="empty-state">暂无粉丝。</div>
        </article>

        <article class="panel block-section">
          <div class="section-title"><h3>关注</h3></div>
          <div v-if="followings.length" class="user-list">
            <UserCard v-for="item in followings" :key="item.uid" :user="item" />
          </div>
          <div v-else class="empty-state">暂无关注。</div>
        </article>

        <article class="panel block-section">
          <div class="section-title"><h3>好友</h3></div>
          <div v-if="friends.length" class="user-list">
            <UserCard v-for="item in friends" :key="item.uid" :user="item" />
          </div>
          <div v-else class="empty-state">暂无互关好友。</div>
        </article>
      </div>
    </section>
  </section>
</template>

<style scoped>
.user-space {
  display: grid;
  gap: 24px;
}

.profile-hero {
  display: grid;
  grid-template-columns: 104px 1fr auto;
  gap: 20px;
  align-items: center;
  padding: 24px;
}

.profile-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.message-link {
  text-decoration: none;
}

.profile-avatar {
  width: 104px;
  height: 104px;
  border-radius: 50%;
  object-fit: cover;
}

.profile-main h1 {
  margin: 0 0 8px;
  font-family: var(--font-heading);
}

.profile-main p {
  margin: 0 0 10px;
  color: var(--muted);
}

.user-grid-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 24px;
}

.block-section {
  padding: 24px;
}

.video-grid,
.user-list,
.side-stack {
  display: grid;
  gap: 16px;
}

.video-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

@media (max-width: 1080px) {
  .profile-hero,
  .user-grid-layout {
    grid-template-columns: 1fr;
  }

  .video-grid {
    grid-template-columns: 1fr;
  }
}
</style>
