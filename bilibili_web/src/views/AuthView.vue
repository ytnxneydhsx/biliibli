<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../lib/api'
import { applyLogin, authState, logout } from '../lib/auth'
import type { UserLoginVO } from '../types'

const route = useRoute()
const router = useRouter()

const tab = ref<'login' | 'register'>('login')
const loading = ref(false)
const error = ref('')

const loginForm = reactive({
  username: '',
  password: '',
})

const registerForm = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: '',
})

const redirectTarget = computed(() => {
  return typeof route.query.redirect === 'string' ? route.query.redirect : '/'
})

async function submitLogin() {
  loading.value = true
  error.value = ''
  try {
    const payload = await api.post<UserLoginVO>('/users/login', loginForm)
    await applyLogin(payload)
    await router.push(redirectTarget.value)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '登录失败'
  } finally {
    loading.value = false
  }
}

async function submitRegister() {
  loading.value = true
  error.value = ''
  try {
    await api.post<number>('/users/register', registerForm)
    tab.value = 'login'
    loginForm.username = registerForm.username
    loginForm.password = registerForm.password
  } catch (err) {
    error.value = err instanceof Error ? err.message : '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="auth-layout">
    <article class="auth-intro panel">
      <span class="tag-chip">账号系统</span>
      <h1>登录后才能调用 `/me/**` 和上传相关接口。</h1>
      <p>
        当前前端只使用后端现有鉴权方式：JWT 写入浏览器本地存储，请求统一带 `Authorization: Bearer ...`。
      </p>
      <div v-if="authState.token && authState.uid" class="session-card">
        <p>当前已登录为 <strong>{{ authState.profile?.nickname || authState.username }}</strong></p>
        <div class="hero-actions">
          <RouterLink class="primary-button" to="/studio">进入创作中心</RouterLink>
          <button class="secondary-button" type="button" @click="logout">退出登录</button>
        </div>
      </div>
    </article>

    <article class="auth-card panel">
      <div class="auth-tabs">
        <button
          :class="tab === 'login' ? 'primary-button' : 'secondary-button'"
          type="button"
          @click="tab = 'login'"
        >
          登录
        </button>
        <button
          :class="tab === 'register' ? 'primary-button' : 'secondary-button'"
          type="button"
          @click="tab = 'register'"
        >
          注册
        </button>
      </div>

      <p v-if="error" class="error-state">{{ error }}</p>

      <form v-if="tab === 'login'" class="field-grid" @submit.prevent="submitLogin">
        <div class="field-group">
          <label for="login-username">用户名</label>
          <input id="login-username" v-model.trim="loginForm.username" required />
        </div>
        <div class="field-group">
          <label for="login-password">密码</label>
          <input id="login-password" v-model="loginForm.password" type="password" required />
        </div>
        <button class="primary-button" type="submit" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>

      <form v-else class="field-grid" @submit.prevent="submitRegister">
        <div class="field-group">
          <label for="register-username">用户名</label>
          <input id="register-username" v-model.trim="registerForm.username" required />
        </div>
        <div class="field-group">
          <label for="register-nickname">昵称</label>
          <input id="register-nickname" v-model.trim="registerForm.nickname" required />
        </div>
        <div class="field-group">
          <label for="register-password">密码</label>
          <input id="register-password" v-model="registerForm.password" type="password" required />
        </div>
        <div class="field-group">
          <label for="register-confirm">确认密码</label>
          <input id="register-confirm" v-model="registerForm.confirmPassword" type="password" required />
        </div>
        <button class="primary-button" type="submit" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>
      </form>
    </article>
  </section>
</template>

<style scoped>
.auth-layout {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 24px;
}

.auth-intro,
.auth-card {
  padding: 28px;
}

.auth-intro h1 {
  margin: 18px 0 12px;
  font-family: var(--font-heading);
  font-size: clamp(30px, 4vw, 50px);
  line-height: 1.06;
}

.auth-intro p {
  color: var(--muted);
}

.auth-tabs {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.session-card {
  margin-top: 24px;
  padding: 18px;
  border-radius: var(--radius-md);
  background: rgba(0, 161, 214, 0.08);
}

@media (max-width: 960px) {
  .auth-layout {
    grid-template-columns: 1fr;
  }
}
</style>
