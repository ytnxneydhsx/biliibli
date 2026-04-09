import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import router from './router'
import { authState } from './lib/auth'

describe('router studio/settings split', () => {
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

  it('redirects guests from /studio and /settings to auth with the redirect query', async () => {
    await router.push('/studio')
    expect(router.currentRoute.value.name).toBe('auth')
    expect(router.currentRoute.value.query.redirect).toBe('/studio')

    await router.push('/settings')
    expect(router.currentRoute.value.name).toBe('auth')
    expect(router.currentRoute.value.query.redirect).toBe('/settings')
  })
})
