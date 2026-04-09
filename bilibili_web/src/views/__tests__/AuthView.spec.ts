import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils'
import { beforeEach, describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import AuthView from '../AuthView.vue'
import { authState } from '../../lib/auth'

const router = createRouter({
  history: createMemoryHistory(),
  routes: [
    { path: '/', name: 'home', component: { template: '<div />' } },
    { path: '/auth', name: 'auth', component: { template: '<div />' } },
    { path: '/studio', name: 'studio', component: { template: '<div />' } },
  ],
})

describe('AuthView', () => {
  beforeEach(async () => {
    authState.token = ''
    authState.uid = null
    authState.username = ''
    authState.profile = null
    authState.ready = true
    await router.push('/auth')
    await router.isReady()
  })

  it('uses community-focused copy for guests without technical details', () => {
    const wrapper = mount(AuthView, {
      global: {
        plugins: [router],
        stubs: {
          RouterLink: RouterLinkStub,
        },
      },
    })

    expect(wrapper.text()).toContain('加入社区')
    expect(wrapper.text()).toContain('发现喜欢的视频')
    expect(wrapper.text()).not.toContain('/me/**')
    expect(wrapper.text()).not.toContain('JWT')
    expect(wrapper.text()).not.toContain('Authorization')
    expect(wrapper.text()).not.toContain('账号系统')
  })

  it('shows a friendly logged-in card instead of a technical session note', () => {
    authState.token = 'token'
    authState.uid = '7'
    authState.username = 'alice'
    authState.profile = {
      uid: '7',
      nickname: '小桃',
      avatar: '',
      sign: '热爱分享日常',
      followerCount: 12,
      followingCount: 8,
    }

    const wrapper = mount(AuthView, {
      global: {
        plugins: [router],
        stubs: {
          RouterLink: RouterLinkStub,
        },
      },
    })

    expect(wrapper.text()).toContain('你已经在社区里啦')
    expect(wrapper.text()).toContain('小桃')
    expect(wrapper.text()).toContain('去首页看看')
    expect(wrapper.text()).toContain('进入创作中心')
    expect(wrapper.text()).not.toContain('当前已登录为')
  })

  it('returns home after logging out from the logged-in welcome card', async () => {
    authState.token = 'token'
    authState.uid = '7'
    authState.username = 'alice'

    const wrapper = mount(AuthView, {
      global: {
        plugins: [router],
        stubs: {
          RouterLink: RouterLinkStub,
        },
      },
    })

    await wrapper.get('.text-button').trigger('click')
    await flushPromises()

    expect(authState.token).toBe('')
    expect(router.currentRoute.value.name).toBe('home')
  })
})
