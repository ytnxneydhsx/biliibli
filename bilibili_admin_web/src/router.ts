import { createRouter, createWebHistory } from 'vue-router'
import AdminShell from './components/AdminShell.vue'
import { authState, isAdmin, logout } from './lib/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: () => (authState.token && isAdmin() ? '/videos' : '/login'),
    },
    {
      path: '/login',
      name: 'admin-login',
      component: () => import('./views/AdminLoginView.vue'),
    },
    {
      path: '/',
      component: AdminShell,
      meta: { requiresAdmin: true },
      children: [
        {
          path: 'videos',
          name: 'admin-videos',
          component: () => import('./views/AdminVideosView.vue'),
        },
        {
          path: 'users',
          name: 'admin-users',
          component: () => import('./views/AdminUsersView.vue'),
        },
      ],
    },
  ],
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach((to) => {
  if (to.meta.requiresAdmin && (!authState.token || !isAdmin())) {
    logout()
    return '/login'
  }

  if (to.path === '/login' && authState.token && isAdmin()) {
    return '/videos'
  }

  return true
})

export default router
