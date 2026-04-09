<template>
  <section class="profile-layout">
    <aside class="panel profile-sidebar">
      <div class="profile-sidebar-copy">
        <p class="profile-eyebrow">个人</p>
        <h1>把你的资料和私信权限放在一起整理。</h1>
        <p class="profile-copy">左边切换资料设置和隐私设置，右边直接修改当前这部分内容。</p>
      </div>

      <nav class="profile-nav">
        <RouterLink :to="{ name: 'profile' }">资料设置</RouterLink>
        <RouterLink :to="{ name: 'profile-privacy' }">隐私设置</RouterLink>
      </nav>
    </aside>

    <div class="profile-content">
      <RouterView />
    </div>
  </section>
</template>

<style scoped>
.profile-layout {
  display: grid;
  grid-template-columns: minmax(220px, 280px) minmax(0, 1fr);
  gap: 24px;
  align-items: start;
}

.profile-sidebar {
  position: sticky;
  top: 112px;
  padding: 24px;
  display: grid;
  gap: 24px;
}

.profile-sidebar-copy {
  display: grid;
  gap: 10px;
}

.profile-eyebrow {
  margin: 0;
  color: #ff6b9f;
  font-size: 0.95rem;
  font-weight: 700;
}

.profile-sidebar h1 {
  margin: 0;
  font-size: clamp(1.6rem, 2.4vw, 2rem);
  line-height: 1.2;
}

.profile-copy {
  margin: 0;
  color: rgba(33, 52, 79, 0.72);
  line-height: 1.7;
}

.profile-nav {
  display: grid;
  gap: 10px;
}

.profile-nav a {
  display: flex;
  align-items: center;
  min-height: 46px;
  padding: 0 16px;
  border-radius: 16px;
  color: rgba(33, 52, 79, 0.72);
  background: rgba(255, 255, 255, 0.64);
  border: 1px solid rgba(127, 168, 214, 0.18);
  transition: background 0.2s ease, color 0.2s ease, border-color 0.2s ease;
}

.profile-nav a.router-link-active,
.profile-nav a:hover {
  color: #21344f;
  background: rgba(255, 255, 255, 0.92);
  border-color: rgba(255, 107, 159, 0.28);
  font-weight: 700;
}

.profile-content {
  min-width: 0;
}

@media (max-width: 920px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }

  .profile-sidebar {
    position: static;
  }
}
</style>
