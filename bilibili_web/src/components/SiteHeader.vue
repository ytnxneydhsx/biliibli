<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authState, logout } from '../lib/auth'

const router = useRouter()
const route = useRoute()
const searchKeyword = ref('')

watch(
  () => route.query.q,
  (value) => {
    searchKeyword.value = typeof value === 'string' ? value : ''
  },
  { immediate: true },
)

const currentAvatar = computed(() => authState.profile?.avatar || '')
const currentNickname = computed(() => authState.profile?.nickname || authState.username || '登录')

function submitSearch() {
  const q = searchKeyword.value.trim()
  router.push({
    name: 'search',
    query: q ? { q } : {},
  })
}

function handleLogout() {
  logout()
  if (route.meta.requiresAuth) {
    router.push('/')
  }
}
</script>

<template>
  <header class="site-header panel">
    <RouterLink class="brand" to="/">
      <div class="brand-mark">bili</div>
      <div class="brand-copy">
        <strong>BiliBili Clone</strong>
        <span>一起看点喜欢的视频</span>
      </div>
    </RouterLink>

    <form class="header-search" @submit.prevent="submitSearch">
      <input
        v-model="searchKeyword"
        type="search"
        placeholder="搜索视频、作者、关键字"
      />
      <button class="primary-button search-submit" type="submit">搜索</button>
    </form>

    <nav class="header-nav">
      <RouterLink to="/">首页</RouterLink>
      <RouterLink :to="{ name: 'search' }">搜索</RouterLink>
      <RouterLink v-if="authState.token" :to="{ name: 'messages' }">私信</RouterLink>
      <RouterLink v-if="authState.token" to="/studio">创作中心</RouterLink>
      <RouterLink v-if="authState.token" to="/profile">个人</RouterLink>
    </nav>

    <div class="header-user">
      <template v-if="authState.token && authState.uid">
        <RouterLink class="user-chip" :to="`/user/${authState.uid}`">
          <img v-if="currentAvatar" :src="currentAvatar" alt="avatar" />
          <span v-else class="fallback-avatar">{{ currentNickname.slice(0, 1) }}</span>
          <span>{{ currentNickname }}</span>
        </RouterLink>
        <button class="secondary-button" type="button" @click="handleLogout">退出</button>
      </template>
      <template v-else>
        <RouterLink class="primary-button" to="/auth">登录 / 注册</RouterLink>
      </template>
    </div>
  </header>
</template>

<style scoped>
.site-header {
  width: min(var(--content-width), calc(100vw - 32px));
  position: sticky;
  top: 16px;
  z-index: 20;
  margin: 16px auto 0;
  padding: 18px 22px;
  display: grid;
  grid-template-columns: 240px minmax(280px, 1fr) auto auto;
  gap: 18px;
  align-items: center;
  background: var(--bg-panel-strong);
}

.brand {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-copy {
  display: grid;
  gap: 4px;
}

.brand strong {
  display: block;
  font-family: var(--font-heading);
  font-size: 20px;
  letter-spacing: -0.02em;
}

.brand span {
  color: var(--muted);
  font-size: 13px;
}

.brand-mark {
  width: 54px;
  height: 54px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #ffdce8 0%, #c4f0ff 100%);
  color: var(--pink);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.7), var(--shadow-soft);
  font-weight: 900;
  font-size: 19px;
}

.header-search {
  display: flex;
  gap: 10px;
}

.search-submit {
  flex: 0 0 auto;
  min-width: 88px;
  white-space: nowrap;
}

.header-search input {
  width: 100%;
  min-width: 0;
  border: 1px solid var(--line);
  border-radius: 999px;
  padding: 13px 18px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.header-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  color: var(--muted);
}

.header-nav a {
  display: inline-flex;
  align-items: center;
  min-height: 38px;
  padding: 0 12px;
  border-radius: 999px;
  transition: background 0.2s ease, color 0.2s ease;
}

.header-nav a:hover,
.header-nav .router-link-active {
  background: rgba(255, 255, 255, 0.72);
  color: var(--text);
  font-weight: 700;
}

.header-user {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 6px 10px 6px 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid var(--line);
}

.user-chip img,
.fallback-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  object-fit: cover;
}

.fallback-avatar {
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, var(--pink) 0%, #ff9ab6 100%);
  color: #fff;
}

@media (max-width: 1100px) {
  .site-header {
    grid-template-columns: 1fr;
  }

  .header-search {
    order: 2;
  }

  .header-nav,
  .header-user {
    justify-content: flex-start;
  }
}

@media (max-width: 960px) {
  .site-header {
    width: min(calc(100vw - 20px), var(--content-width));
    padding: 14px;
  }

  .header-search {
    flex-wrap: nowrap;
  }

  .header-nav {
    flex-wrap: wrap;
  }
}
</style>
