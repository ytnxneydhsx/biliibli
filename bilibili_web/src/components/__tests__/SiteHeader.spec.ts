import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import SiteHeader from '../SiteHeader.vue'
import router from '../../router'
import { authState } from '../../lib/auth'

describe('SiteHeader', () => {
  beforeEach(async () => {
    vi.stubGlobal('scrollTo', vi.fn())
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
      },
    })
    const links = wrapper.findAll('a')
    const authLink = links.find((link) => link.text() === '登录 / 注册')

    expect(authLink?.attributes('href')).toBe('/auth')
    expect(wrapper.text()).toContain('登录 / 注册')
    expect(wrapper.text()).toContain('一起看点喜欢的视频')
    expect(wrapper.text()).not.toContain('Spring Boot')
    expect(wrapper.text()).not.toContain('Vue')
    expect(wrapper.get('input[type="search"]').attributes('placeholder')).toBe('搜索视频、作者、关键字')
    expect(wrapper.get('.header-search .primary-button').text()).toBe('搜索')
  })

  it('routes 创作中心 to /studio and 资料设置 to /settings in the real app router', async () => {
    authState.token = 'token'
    authState.uid = '7'
    authState.username = 'alice'

    const wrapper = mount(SiteHeader, {
      global: {
        plugins: [router],
      },
    })

    const links = wrapper.findAll('a')
    const studioLink = links.find((link) => link.text() === '创作中心')
    const settingsLink = links.find((link) => link.text() === '资料设置')

    expect(studioLink?.attributes('href')).toBe('/studio')
    expect(settingsLink?.attributes('href')).toBe('/settings')

    await settingsLink?.trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.name).toBe('settings')
    expect(router.currentRoute.value.path).toBe('/settings')
  })
})
