import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import SettingsView from '../SettingsView.vue'
import { authState } from '../../lib/auth'

const { refreshCurrentUser, api } = vi.hoisted(() => ({
  refreshCurrentUser: vi.fn(async () => undefined),
  api: {
    put: vi.fn(),
    post: vi.fn(),
  },
}))

vi.mock('../../lib/auth', async () => {
  const actual = await vi.importActual<typeof import('../../lib/auth')>('../../lib/auth')
  return {
    ...actual,
    refreshCurrentUser,
  }
})

vi.mock('../../lib/api', () => ({
  api,
}))

describe('SettingsView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    authState.token = 'token'
    authState.uid = '7'
    authState.username = 'alice'
    authState.profile = {
      uid: '7',
      nickname: '小桃',
      avatar: 'https://example.com/avatar.png',
      sign: '热爱分享日常',
      followerCount: 12,
      followingCount: 8,
    }
  })

  it('shows profile editing controls without studio upload fields', () => {
    const wrapper = mount(SettingsView)

    expect(wrapper.text()).toContain('资料设置')
    expect(wrapper.text()).toContain('修改头像')
    expect(wrapper.get('#nickname').element.value).toBe('小桃')
    expect(wrapper.get('#sign').element.value).toBe('热爱分享日常')
    expect(wrapper.text()).not.toContain('上传视频')
    expect(wrapper.text()).not.toContain('上传并发布')
    expect(wrapper.text()).not.toContain('视频封面')
    expect(wrapper.find('#video-title').exists()).toBe(false)
  })

  it('saves profile edits through the existing profile endpoint', async () => {
    let savedPayload: { nickname: string; sign: string } | null = null
    api.put.mockImplementation(async (_url: string, payload: { nickname: string; sign: string }) => {
      savedPayload = { ...payload }
    })
    const wrapper = mount(SettingsView)

    const nicknameInput = wrapper.get('#nickname')
    const signInput = wrapper.get('#sign')

    await nicknameInput.setValue('新昵称')
    await signInput.setValue('新的签名')
    await flushPromises()
    await wrapper.get('button').trigger('click')
    await flushPromises()

    expect(api.put).toHaveBeenCalledWith('/me/profile', expect.any(Object))
    expect(savedPayload).toEqual({
      nickname: '新昵称',
      sign: '新的签名',
    })
    expect(refreshCurrentUser).toHaveBeenCalled()
    expect(wrapper.text()).toContain('资料已更新')
  })

  it('uploads avatar through the existing avatar endpoint', async () => {
    api.post.mockResolvedValue('https://example.com/new-avatar.png')
    const wrapper = mount(SettingsView)
    const avatarInput = wrapper.get('input[type="file"]')
    const file = new File(['avatar'], 'avatar.png', { type: 'image/png' })

    Object.defineProperty(avatarInput.element, 'files', {
      value: [file],
      configurable: true,
    })

    await avatarInput.trigger('change')
    await flushPromises()

    expect(api.post).toHaveBeenCalledWith(
      '/me/uploads/avatar',
      expect.any(FormData),
      { headers: { 'Content-Type': 'multipart/form-data' } },
    )
    expect(refreshCurrentUser).toHaveBeenCalled()
    expect(wrapper.text()).toContain('头像上传成功')
  })
})
