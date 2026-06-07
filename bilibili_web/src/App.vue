<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import SiteHeader from './components/SiteHeader.vue'

const route = useRoute()
const hideSiteHeader = computed(() => Boolean(route.meta.hideSiteHeader))
const fullBleedPage = computed(() => Boolean(route.meta.fullBleed))
</script>

<template>
  <div class="app-shell" :class="{ 'app-shell-full': fullBleedPage }">
    <SiteHeader v-if="!hideSiteHeader" />
    <main class="page-shell" :class="{ 'page-shell-full': fullBleedPage }">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.app-shell-full {
  min-height: 100vh;
}

.page-shell-full {
  width: 100%;
  min-height: 100vh;
  padding: 0;
}
</style>
