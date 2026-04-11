<script setup lang="ts">
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { logout } from '../lib/auth'

const router = useRouter()

function handleLogout() {
  logout()
  router.replace('/login')
}
</script>

<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="admin-brand">Bilibili Admin</div>
      <nav class="admin-nav">
        <RouterLink to="/videos">视频管理</RouterLink>
        <RouterLink to="/users">用户管理</RouterLink>
      </nav>
      <button class="ghost-button" type="button" @click="handleLogout">退出登录</button>
    </aside>

    <main class="admin-main">
      <RouterView />
    </main>
  </div>
</template>
