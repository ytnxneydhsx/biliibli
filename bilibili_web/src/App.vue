<script setup lang="ts">
import SiteHeader from './components/SiteHeader.vue'
</script>

<template>
  <div class="app-shell">
    <SiteHeader />
    <main class="page-shell">
      <RouterView />
    </main>
  </div>
</template>
