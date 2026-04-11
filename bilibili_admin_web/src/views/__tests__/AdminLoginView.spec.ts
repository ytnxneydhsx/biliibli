import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AdminLoginView from '../AdminLoginView.vue'

const { push, replace, loginAsAdmin } = vi.hoisted(() => ({
  push: vi.fn(),
  replace: vi.fn(),
  loginAsAdmin: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push,
    replace,
  }),
}))

vi.mock('../../lib/auth', async () => {
  const actual = await vi.importActual<typeof import('../../lib/auth')>('../../lib/auth')
  return {
    ...actual,
    loginAsAdmin,
  }
})

describe('AdminLoginView', () => {
  beforeEach(() => {
    push.mockReset()
    replace.mockReset()
    loginAsAdmin.mockReset()
  })

  it('redirects to /videos after admin login', async () => {
    loginAsAdmin.mockResolvedValue(undefined)

    const wrapper = mount(AdminLoginView)
    await wrapper.get('input[placeholder="用户名"]').setValue('admin')
    await wrapper.get('input[placeholder="密码"]').setValue('secret')
    await wrapper.get('form').trigger('submit.prevent')
    await flushPromises()

    expect(loginAsAdmin).toHaveBeenCalledWith({
      username: 'admin',
      password: 'secret',
    })
    expect(replace).toHaveBeenCalledWith('/videos')
  })

  it('shows permission error when loginAsAdmin rejects', async () => {
    loginAsAdmin.mockRejectedValue(new Error('你没有管理员权限'))

    const wrapper = mount(AdminLoginView)
    await wrapper.get('input[placeholder="用户名"]').setValue('user')
    await wrapper.get('input[placeholder="密码"]').setValue('secret')
    await wrapper.get('form').trigger('submit.prevent')
    await flushPromises()

    expect(wrapper.text()).toContain('你没有管理员权限')
  })
})
