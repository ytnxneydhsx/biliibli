import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils'
import { beforeEach, describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import SiteHeader from '../SiteHeader.vue'
import { authState } from '../../lib/auth'

const router = createRouter({
  history: createMemoryHistory(),
  routes: [
    { path: '/', name: 'home', component: { template: '<div />' } },
    { path: '/search', name: 'search', component: { template: '<div />' } },
    { path: '/auth', name: 'auth', component: { template: '<div />' } },
    { path: '/studio', name: 'studio', component: { template: '<div />' } },
  ],
})

describe('SiteHeader', () => {
  beforeEach(async () => {
    authState.token = ''
    authState.uid = ''
    authState.username = ''
    authState.profile = null
    await router.push('/')
    await router.isReady()
  })

  it('keeps the real guest entry while using the refreshed brand and search copy', async () => {
    const wrapper = mount(SiteHeader, {
      global: {
        plugins: [router],
        stubs: {
          RouterLink: RouterLinkStub,
        },
      },
    })

    await wrapper.get('input[type="search"]').setValue('动画')
    await wrapper.get('form').trigger('submit.prevent')
    await flushPromises()

    expect(router.currentRoute.value.name).toBe('search')
    expect(router.currentRoute.value.query.q).toBe('动画')
    expect(wrapper.text()).toContain('登录 / 注册')
    expect(wrapper.text()).toContain('青春内容站')
    expect(wrapper.get('input[type="search"]').attributes('placeholder')).toBe('搜索视频、作者、关键字')
  })
})
