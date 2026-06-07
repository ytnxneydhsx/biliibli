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

const currentNickname = computed(() => authState.profile?.nickname || authState.username || '你')
const welcomeNote = computed(() => {
  return authState.profile?.sign?.trim() || '挑一条想看的视频，或者去看看大家最近在分享什么。'
})

function handleLogout() {
  logout()
  router.push('/')
}

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
      <span class="tag-chip">加入社区</span>
      <h1>发现喜欢的视频，认识有趣的创作者。</h1>
      <p class="auth-copy">
        登录之后，你可以继续追更、看看大家的分享，也能把自己喜欢的内容慢慢收藏起来。
      </p>

      <div class="auth-highlights">
        <div class="highlight-pill">看看最近更新了什么</div>
        <div class="highlight-pill">认识更多有意思的 UP 主</div>
        <div class="highlight-pill">把喜欢的内容留在自己的主页里</div>
      </div>

      <div v-if="authState.token && authState.uid" class="session-card">
        <div class="session-copy">
          <span class="session-label">你已经在社区里啦</span>
          <h2>{{ currentNickname }}，欢迎回来</h2>
          <p>{{ welcomeNote }}</p>
        </div>
        <div class="session-actions">
          <RouterLink class="secondary-button" to="/">去首页看看</RouterLink>
          <RouterLink class="primary-button" to="/studio">进入创作中心</RouterLink>
          <button class="text-button" type="button" @click="handleLogout">退出登录</button>
        </div>
      </div>
    </article>

    <article class="auth-card panel">
      <div class="auth-card-head">
        <div>
          <h2>{{ tab === 'login' ? '欢迎回来' : '创建你的账号' }}</h2>
          <p class="muted">
            {{ tab === 'login' ? '继续追更、看视频、逛社区。' : '几步就能加入这里，开始认识更多有趣的人。' }}
          </p>
        </div>
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
      </div>

      <p v-if="error" class="error-state">{{ error }}</p>

      <form v-if="tab === 'login'" class="field-grid" @submit.prevent="submitLogin">
        <div class="field-group">
          <label for="login-username">用户名</label>
          <input id="login-username" v-model.trim="loginForm.username" placeholder="输入你的用户名" required />
        </div>
        <div class="field-group">
          <label for="login-password">密码</label>
          <input id="login-password" v-model="loginForm.password" type="password" placeholder="输入登录密码" required />
        </div>
        <button class="primary-button auth-submit" type="submit" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>

      <form v-else class="field-grid" @submit.prevent="submitRegister">
        <div class="field-group">
          <label for="register-username">用户名</label>
          <input id="register-username" v-model.trim="registerForm.username" placeholder="给自己起一个用户名" required />
        </div>
        <div class="field-group">
          <label for="register-nickname">昵称</label>
          <input id="register-nickname" v-model.trim="registerForm.nickname" placeholder="大家会看到的名字" required />
        </div>
        <div class="field-group">
          <label for="register-password">密码</label>
          <input id="register-password" v-model="registerForm.password" type="password" placeholder="设置登录密码" required />
        </div>
        <div class="field-group">
          <label for="register-confirm">确认密码</label>
          <input id="register-confirm" v-model="registerForm.confirmPassword" type="password" placeholder="再输入一次密码" required />
        </div>
        <button class="primary-button auth-submit" type="submit" :disabled="loading">
          {{ loading ? '注册中...' : '注册并加入' }}
        </button>
      </form>
    </article>
  </section>
</template>

<style scoped>
.auth-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(360px, 0.92fr);
  gap: 24px;
  align-items: center;
  min-height: 560px;
}

.auth-intro,
.auth-card {
  padding: 34px;
}

.auth-intro {
  position: relative;
  overflow: hidden;
  min-height: 420px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  background:
    linear-gradient(145deg, rgba(16, 27, 41, 0.92) 0%, rgba(10, 21, 34, 0.9) 58%, rgba(12, 35, 52, 0.86) 100%);
}

.auth-intro::before {
  content: "";
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 12% 12%, rgba(251, 114, 153, 0.22), transparent 30%),
    radial-gradient(circle at 88% 88%, rgba(50, 197, 255, 0.16), transparent 34%),
    linear-gradient(120deg, rgba(255, 255, 255, 0.05), transparent 42%);
  pointer-events: none;
}

.auth-intro > * {
  position: relative;
  z-index: 1;
}

.auth-intro h1 {
  margin: 18px 0 12px;
  max-width: 12ch;
  font-family: var(--font-heading);
  font-size: clamp(34px, 4vw, 58px);
  line-height: 1.06;
  color: var(--text);
  overflow-wrap: anywhere;
  word-break: break-word;
}

.auth-copy {
  max-width: 46ch;
  color: var(--muted);
  overflow-wrap: anywhere;
}

.auth-highlights {
  display: grid;
  gap: 12px;
  margin-top: 26px;
}

.highlight-pill {
  display: inline-flex;
  align-items: center;
  min-height: 44px;
  padding: 0 16px;
  width: fit-content;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid var(--line-strong);
  color: var(--text);
  box-shadow: 0 14px 32px rgba(0, 0, 0, 0.2);
}

.session-card {
  margin-top: 28px;
  padding: 22px;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid var(--line-strong);
  box-shadow: var(--shadow-soft);
  display: grid;
  gap: 18px;
}

.session-copy {
  display: grid;
  gap: 8px;
}

.session-label {
  color: var(--blue);
  font-size: 13px;
  font-weight: 700;
}

.session-copy h2 {
  margin: 0;
  font-family: var(--font-heading);
  font-size: 30px;
}

.session-copy p {
  margin: 0;
  color: var(--muted);
}

.session-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.auth-card {
  position: relative;
  overflow: hidden;
  align-self: center;
  background:
    linear-gradient(150deg, rgba(18, 29, 43, 0.96) 0%, rgba(12, 22, 35, 0.94) 100%);
}

.auth-card::before {
  content: "";
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 18% 0%, rgba(251, 114, 153, 0.18), transparent 28%),
    radial-gradient(circle at 100% 20%, rgba(50, 197, 255, 0.12), transparent 32%);
  pointer-events: none;
}

.auth-card > * {
  position: relative;
  z-index: 1;
}

.auth-card-head {
  display: grid;
  gap: 18px;
  margin-bottom: 22px;
}

.auth-card-head h2 {
  margin: 0 0 6px;
  font-family: var(--font-heading);
  font-size: 28px;
  color: var(--text);
}

.auth-card-head p {
  margin: 0;
}

.auth-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.auth-submit {
  width: 100%;
  min-height: 46px;
}

.auth-card .field-group label {
  color: #d8e6f6;
  font-size: 14px;
}

.auth-card .field-group input {
  min-height: 48px;
  border-color: rgba(187, 208, 235, 0.18);
  background: rgba(5, 13, 23, 0.48);
  color: var(--text);
  outline: none;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

.auth-card .field-group input::placeholder {
  color: rgba(216, 230, 246, 0.42);
}

.auth-card .field-group input:focus {
  border-color: rgba(251, 114, 153, 0.62);
  box-shadow:
    0 0 0 3px rgba(251, 114, 153, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

@media (max-width: 960px) {
  .auth-layout {
    grid-template-columns: 1fr;
    min-height: unset;
    align-items: stretch;
  }

  .auth-intro,
  .auth-card {
    padding: 24px;
  }

  .auth-intro {
    min-height: 340px;
  }
}

@media (max-width: 560px) {
  .auth-intro,
  .auth-card {
    padding: 20px;
  }

  .auth-intro h1 {
    max-width: 9em;
    font-size: 32px;
  }

  .auth-tabs,
  .session-actions {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }

  .session-actions .text-button {
    grid-column: 1 / -1;
    justify-self: start;
  }
}
</style>
