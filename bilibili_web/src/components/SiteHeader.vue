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
      <div class="brand-mark">▶</div>
      <div class="brand-copy">
        <strong>视界</strong>
        <span>发现好看的视频</span>
      </div>
    </RouterLink>

    <nav class="header-nav">
      <RouterLink to="/">首页</RouterLink>
      <RouterLink :to="{ name: 'search' }">搜索</RouterLink>
      <RouterLink :to="{ name: 'messages' }">聊天室</RouterLink>
    </nav>

    <form class="header-search" @submit.prevent="submitSearch">
      <input
        v-model="searchKeyword"
        type="search"
        placeholder="搜索视频、作者、关键字"
      />
      <button class="search-submit" type="submit">搜索</button>
    </form>

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
  top: 14px;
  z-index: 20;
  margin: 14px auto 0;
  padding: 12px 18px;
  display: grid;
  grid-template-columns: 150px auto minmax(360px, 520px) auto;
  gap: 26px;
  align-items: center;
  background: var(--bg-panel-strong);
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.24);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.brand-copy {
  display: grid;
  gap: 2px;
}

.brand strong {
  display: block;
  font-family: var(--font-heading);
  font-size: 23px;
  letter-spacing: 0;
}

.brand span {
  color: var(--muted);
  font-size: 12px;
}

.brand-mark {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #ff5f8f 0%, #fb7299 100%);
  color: #fff;
  box-shadow: 0 12px 24px rgba(251, 114, 153, 0.24);
  font-weight: 900;
  font-size: 15px;
  line-height: 1;
}

.header-search {
  position: relative;
  display: flex;
  align-items: center;
  min-width: 0;
  max-width: 100%;
  justify-self: center;
  width: 100%;
}

.search-submit {
  position: absolute;
  right: 6px;
  flex: 0 0 auto;
  min-width: 70px;
  min-height: 34px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.1);
  color: #d9e8f7;
  white-space: nowrap;
}

.header-search input {
  width: 100%;
  min-width: 0;
  border: 1px solid rgba(165, 188, 218, 0.12);
  border-radius: 999px;
  padding: 12px 86px 12px 18px;
  background: rgba(255, 255, 255, 0.06);
  color: var(--text);
  outline: none;
}

.header-search input::placeholder {
  color: #71859d;
}

.header-search input:focus {
  border-color: rgba(50, 197, 255, 0.45);
  box-shadow: 0 0 0 3px rgba(50, 197, 255, 0.12);
}

.header-nav {
  display: flex;
  flex-wrap: nowrap;
  gap: 20px;
  color: var(--muted);
  min-width: 0;
}

.header-nav a {
  display: inline-flex;
  align-items: center;
  min-height: 36px;
  padding: 0 2px;
  border-radius: 0;
  white-space: nowrap;
  border-bottom: 2px solid transparent;
  transition: background 0.2s ease, color 0.2s ease;
}

.header-nav a:hover,
.header-nav .router-link-active {
  background: transparent;
  border-bottom-color: var(--pink);
  color: var(--text);
  font-weight: 700;
}

.header-user {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  min-width: 0;
}

.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 6px 10px 6px 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
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
    grid-template-columns: 1fr auto;
  }

  .header-search {
    grid-column: 1 / -1;
    order: 2;
  }

  .header-nav {
    grid-column: 1 / -1;
    order: 3;
    gap: 18px;
  }

  .header-user {
    justify-content: flex-start;
  }
}

@media (max-width: 960px) {
  .site-header {
    width: min(calc(100vw - 20px), var(--content-width));
    padding: 14px;
    overflow: hidden;
  }

  .header-search {
    flex-wrap: nowrap;
  }

  .header-nav {
    flex-wrap: wrap;
  }
}

@media (max-width: 560px) {
  .site-header {
    grid-template-columns: 1fr;
  }

  .header-user {
    justify-content: flex-start;
  }

  .header-nav {
    overflow-x: auto;
  }

  .brand-copy span {
    display: none;
  }

  .header-search input {
    padding-right: 18px;
  }

  .search-submit {
    display: none;
  }
}
</style>
