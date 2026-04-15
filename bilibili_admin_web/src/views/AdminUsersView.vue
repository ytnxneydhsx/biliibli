<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '../lib/api'
import type { AdminUserVO, PageVO } from '../types'

const users = ref<AdminUserVO[]>([])
const loading = ref(false)
const errorMessage = ref('')
const pageNo = ref(1)
const pageSize = 10
const totalPages = ref(0)
const total = ref(0)
const keyword = ref('')
const submittingUserId = ref<string | null>(null)
const query = reactive({ keyword: '' })

const summaryText = computed(() => `共 ${total.value} 位用户，第 ${pageNo.value} / ${Math.max(totalPages.value, 1)} 页`)

function roleLabel(roleCode: number) {
  return roleCode === 1 ? '管理员' : '普通用户'
}

function statusLabel(status: number) {
  return status === 0 ? '正常' : '已停用'
}

async function loadUsers() {
  loading.value = true
  errorMessage.value = ''

  try {
    const params: Record<string, unknown> = {
      pageNo: pageNo.value,
      pageSize,
    }
    if (keyword.value.trim()) {
      params.keyword = keyword.value.trim()
    }

    const result = await api.get<PageVO<AdminUserVO>>('/admin/users', params)
    users.value = result.records ?? []
    total.value = result.total ?? 0
    totalPages.value = result.totalPages ?? 0
    pageNo.value = result.pageNo ?? pageNo.value
  } catch (error) {
    errorMessage.value = (error as Error).message
    users.value = []
    total.value = 0
    totalPages.value = 0
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  pageNo.value = 1
  keyword.value = query.keyword.trim()
  await loadUsers()
}

async function changePage(nextPageNo: number) {
  if (nextPageNo < 1 || (totalPages.value > 0 && nextPageNo > totalPages.value) || nextPageNo === pageNo.value) {
    return
  }

  pageNo.value = nextPageNo
  await loadUsers()
}

async function banUser(uid: string) {
  submittingUserId.value = uid
  errorMessage.value = ''
  try {
    await api.post(`/admin/users/${uid}/video-business-ban`)
    await loadUsers()
  } catch (error) {
    errorMessage.value = (error as Error).message
  } finally {
    submittingUserId.value = null
  }
}

async function unbanUser(uid: string) {
  submittingUserId.value = uid
  errorMessage.value = ''
  try {
    await api.delete(`/admin/users/${uid}/video-business-ban`)
    await loadUsers()
  } catch (error) {
    errorMessage.value = (error as Error).message
  } finally {
    submittingUserId.value = null
  }
}

onMounted(() => {
  void loadUsers()
})
</script>

<template>
  <section class="page-section">
    <header class="page-header">
      <div>
        <h1>用户管理</h1>
        <p>查看用户账号、权限状态，并执行视频业务封禁或解禁。</p>
      </div>
    </header>

    <section class="panel">
      <div class="panel-header">
        <div>
          <h2>用户列表</h2>
          <p>{{ summaryText }}</p>
        </div>
        <form class="admin-search-bar" @submit.prevent="handleSearch">
          <input v-model="query.keyword" type="search" placeholder="搜索用户名或 UID" />
          <button class="refresh-button" type="submit">搜索</button>
        </form>
      </div>

      <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

      <div v-if="loading && users.length === 0" class="empty-state">
        <h3>正在加载用户列表…</h3>
        <p>管理员用户数据正在从后端同步。</p>
      </div>

      <div v-else-if="users.length === 0" class="empty-state">
        <h3>当前没有匹配用户</h3>
        <p>你可以调整关键词后再次搜索。</p>
      </div>

      <ul v-else class="user-list">
        <li v-for="user in users" :key="user.uid" class="user-item">
          <div class="user-main">
            <div class="user-title-row">
              <h3>{{ user.username }}</h3>
              <span class="status-badge">{{ roleLabel(user.roleCode) }}</span>
              <span class="status-badge user-ban-badge" :class="{ 'is-banned': user.videoBusinessBanned }">
                {{ user.videoBusinessBanned ? '已封禁' : '未封禁' }}
              </span>
            </div>
            <p class="video-subtitle">UID：{{ user.uid }} · 昵称：{{ user.nickname || '未设置昵称' }}</p>
            <p class="video-subtitle">账号状态：{{ statusLabel(user.status) }} · 个签：{{ user.sign || '暂无个签' }}</p>
            <p class="video-subtitle">
              点赞：{{ user.likeEnabled ? '开' : '关' }} · 评论：{{ user.commentEnabled ? '开' : '关' }} · 投稿：{{ user.videoUploadEnabled ? '开' : '关' }}
            </p>
          </div>
          <div class="user-actions">
            <button
              v-if="!user.videoBusinessBanned"
              class="reject-button"
              data-user-action="ban"
              type="button"
              :disabled="submittingUserId === user.uid"
              @click="banUser(user.uid)"
            >
              封禁
            </button>
            <button
              v-else
              class="approve-button"
              data-user-action="unban"
              type="button"
              :disabled="submittingUserId === user.uid"
              @click="unbanUser(user.uid)"
            >
              解禁
            </button>
          </div>
        </li>
      </ul>

      <div class="pagination-row">
        <button class="refresh-button" type="button" :disabled="pageNo <= 1 || loading" @click="changePage(pageNo - 1)">上一页</button>
        <span class="hint-text">{{ summaryText }}</span>
        <button
          class="refresh-button"
          type="button"
          :disabled="loading || totalPages === 0 || pageNo >= totalPages"
          @click="changePage(pageNo + 1)"
        >
          下一页
        </button>
      </div>
    </section>
  </section>
</template>
