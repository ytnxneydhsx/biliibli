import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import router from './router'
import { authState } from './lib/auth'

describe('router studio/profile split', () => {
  beforeEach(async () => {
    vi.stubGlobal('scrollTo', vi.fn())
    authState.token = ''
    authState.uid = null
    authState.username = ''
    authState.profile = null
    await router.push('/')
    await router.isReady()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('redirects guests from /studio, /profile, and /profile/privacy to auth with the redirect query', async () => {
    await router.push('/studio')
    expect(router.currentRoute.value.name).toBe('auth')
    expect(router.currentRoute.value.query.redirect).toBe('/studio')

    await router.push('/profile')
    expect(router.currentRoute.value.name).toBe('auth')
    expect(router.currentRoute.value.query.redirect).toBe('/profile')

    await router.push('/profile/privacy')
    expect(router.currentRoute.value.name).toBe('auth')
    expect(router.currentRoute.value.query.redirect).toBe('/profile/privacy')
  })

  it('resolves /profile and /profile/privacy to dedicated authenticated routes', async () => {
    authState.token = 'token'
    authState.uid = '7'
    authState.username = 'alice'

    await router.push('/profile')

    expect(router.currentRoute.value.name).toBe('profile')
    expect(router.currentRoute.value.path).toBe('/profile')

    await router.push('/profile/privacy')

    expect(router.currentRoute.value.name).toBe('profile-privacy')
    expect(router.currentRoute.value.path).toBe('/profile/privacy')
  })
})
