<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { loginAsAdmin } from '../lib/auth'

const router = useRouter()
const form = reactive({
  username: '',
  password: '',
})
const errorMessage = ref('')
const submitting = ref(false)

async function handleSubmit() {
  errorMessage.value = ''
  submitting.value = true

  try {
    await loginAsAdmin({
      username: form.username,
      password: form.password,
    })
    await router.replace('/videos')
  } catch (error) {
    errorMessage.value = (error as Error).message
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="admin-login-page">
    <form class="admin-login-card" @submit.prevent="handleSubmit">
      <h1>管理员登录</h1>
      <p>使用已有账号登录，仅管理员可进入后台。</p>

      <input v-model.trim="form.username" placeholder="用户名" />
      <input v-model="form.password" type="password" placeholder="密码" />

      <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

      <button type="submit" :disabled="submitting">
        {{ submitting ? '登录中...' : '登录' }}
      </button>
    </form>
  </section>
</template>
