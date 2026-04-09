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
    expect(wrapper.text()).toContain('一起看点喜欢的视频')
    expect(wrapper.text()).not.toContain('Spring Boot')
    expect(wrapper.text()).not.toContain('Vue')
    expect(wrapper.get('input[type="search"]').attributes('placeholder')).toBe('搜索视频、作者、关键字')
    expect(wrapper.get('.header-search .primary-button').text()).toBe('搜索')
  })

  it('routes 创作中心 to /studio and 资料设置 to settings', async () => {
    authState.token = 'token'
    authState.uid = '7'
    authState.username = 'alice'

    const wrapper = mount(SiteHeader, {
      global: {
        plugins: [router],
        stubs: {
          RouterLink: RouterLinkStub,
        },
      },
    })

    const links = wrapper.findAllComponents(RouterLinkStub)
    const studioLink = links.find((component) => component.text() === '创作中心')
    const settingsLink = links.find((component) => component.text() === '资料设置')

    expect(studioLink?.props('to')).toBe('/studio')
    expect(settingsLink?.props('to')).toEqual({ name: 'settings' })
  })
})
