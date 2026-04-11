import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import router from './router'
import { authState, logout } from './lib/auth'

describe('admin router', () => {
  beforeEach(() => {
    window.scrollTo = vi.fn()
    logout()
  })

  afterEach(async () => {
    await router.replace('/login')
  })

  it('redirects guests to login', async () => {
    await router.push('/videos')
    expect(router.currentRoute.value.fullPath).toBe('/login')
  })

  it('redirects authenticated admin from root to videos', async () => {
    authState.token = 'token'
    authState.roleCode = 1

    await router.push('/')
    expect(router.currentRoute.value.fullPath).toBe('/videos')
  })
})
