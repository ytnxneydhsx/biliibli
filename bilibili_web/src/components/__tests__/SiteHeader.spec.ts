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

  it('routes 创作中心 to /studio and 个人 to /profile in the real app router', async () => {
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
    const profileLink = links.find((link) => link.text() === '个人')

    expect(studioLink?.attributes('href')).toBe('/studio')
    expect(profileLink?.attributes('href')).toBe('/profile')

    await router.push(profileLink?.attributes('href') || '/profile')

    expect(router.currentRoute.value.name).toBe('profile')
    expect(router.currentRoute.value.path).toBe('/profile')
  })

  it('returns home after logout from a protected route', async () => {
    authState.token = 'token'
    authState.uid = '7'
    authState.username = 'alice'
    await router.push('/profile')

    const wrapper = mount(SiteHeader, {
      global: {
        plugins: [router],
      },
    })

    await wrapper.get('.header-user .secondary-button').trigger('click')
    await flushPromises()

    expect(authState.token).toBe('')
    expect(router.currentRoute.value.name).toBe('home')
    expect(router.currentRoute.value.path).toBe('/')
  })
})
