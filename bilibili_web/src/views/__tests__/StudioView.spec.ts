import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import StudioView from '../StudioView.vue'

const { api } = vi.hoisted(() => ({
  api: {
    post: vi.fn(),
    delete: vi.fn(),
  },
}))

vi.mock('../../lib/api', () => ({
  api,
}))

describe('StudioView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('shows upload controls without profile editing fields', () => {
    const wrapper = mount(StudioView)

    expect(wrapper.text()).toContain('创作中心')
    expect(wrapper.text()).toContain('上传并发布')
    expect(wrapper.text()).toContain('上传封面')
    expect(wrapper.find('#video-title').exists()).toBe(true)
    expect(wrapper.find('#video-desc').exists()).toBe(true)
    expect(wrapper.text()).not.toContain('资料设置')
    expect(wrapper.text()).not.toContain('修改头像')
    expect(wrapper.find('#nickname').exists()).toBe(false)
    expect(wrapper.find('#sign').exists()).toBe(false)
  })

  it('keeps the existing upload validation for missing video files', async () => {
    const wrapper = mount(StudioView)

    await wrapper.get('#video-title').setValue('今天也想发个新视频')
    await wrapper.get('.status-line .primary-button').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('请先选择视频文件')
    expect(api.post).not.toHaveBeenCalled()
  })
})
