<script setup lang="ts">
import type { FollowersQueryVO, UserSearchVO } from '../types'

defineProps<{
  user: FollowersQueryVO | UserSearchVO
}>()
</script>

<template>
  <RouterLink class="user-card panel" :to="`/user/${user.uid}`">
    <img class="user-avatar" :src="user.avatar" :alt="user.nickname" />
    <div class="user-body">
      <strong>{{ user.nickname }}</strong>
      <p>{{ user.sign || '这个用户还没有签名。' }}</p>
    </div>
  </RouterLink>
</template>

<style scoped>
.user-card {
  display: flex;
  gap: 14px;
  padding: 14px;
  align-items: center;
}

.user-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  object-fit: cover;
  background: rgba(103, 112, 131, 0.12);
}

.user-body strong {
  display: block;
  margin-bottom: 6px;
}

.user-body p {
  margin: 0;
  color: var(--muted);
  font-size: 14px;
}
</style>
