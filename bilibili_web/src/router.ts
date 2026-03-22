import { createRouter, createWebHistory } from 'vue-router'
import { authState } from './lib/auth'

const routes = [
  {
    path: '/',
    name: 'home',
    component: () => import('./views/HomeView.vue'),
  },
  {
    path: '/auth',
    name: 'auth',
    component: () => import('./views/AuthView.vue'),
  },
  {
    path: '/search',
    name: 'search',
    component: () => import('./views/SearchView.vue'),
  },
  {
    path: '/video/:id',
    name: 'video',
    component: () => import('./views/VideoDetailView.vue'),
  },
  {
    path: '/user/:uid',
    name: 'user',
    component: () => import('./views/UserSpaceView.vue'),
  },
  {
    path: '/studio',
    name: 'studio',
    component: () => import('./views/StudioView.vue'),
    meta: { requiresAuth: true },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !authState.token) {
    return {
      name: 'auth',
      query: { redirect: to.fullPath },
    }
  }
  return true
})

export default router
