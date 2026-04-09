import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ProfilePrivacyView from '../ProfilePrivacyView.vue'
import { authState } from '../../lib/auth'

const { api } = vi.hoisted(() => ({
  api: {
    get: vi.fn(),
    put: vi.fn(),
  },
}))

vi.mock('../../lib/api', () => ({
  api,
}))

describe('ProfilePrivacyView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    authState.token = 'token'
    authState.uid = '7'
    authState.username = 'alice'
    api.get.mockResolvedValue({
      userId: '7',
      privateMessagePolicy: 2,
    })
  })

  it('loads the current private message policy from the existing privacy endpoint', async () => {
    const wrapper = mount(ProfilePrivacyView)

    await flushPromises()

    expect(api.get).toHaveBeenCalledWith('/me/im/privacy', undefined)
    expect(wrapper.text()).toContain('隐私设置')
    expect((wrapper.find('input[value="2"]').element as HTMLInputElement).checked).toBe(true)
  })

  it('saves private message policy changes through the existing privacy endpoint', async () => {
    api.put.mockResolvedValue(undefined)
    const wrapper = mount(ProfilePrivacyView)

    await flushPromises()
    await wrapper.get('input[value="4"]').setValue(true)
    await wrapper.get('button').trigger('click')
    await flushPromises()

    expect(api.put).toHaveBeenCalledWith('/me/im/privacy', {
      privateMessagePolicy: 4,
    })
    expect(wrapper.text()).toContain('隐私设置已更新')
  })
})
