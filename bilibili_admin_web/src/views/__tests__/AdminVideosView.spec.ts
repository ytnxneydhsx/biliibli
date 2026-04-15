import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AdminVideosView from '../AdminVideosView.vue'

const { get, put } = vi.hoisted(() => ({
  get: vi.fn(),
  put: vi.fn(),
}))

vi.mock('../../lib/api', () => ({
  api: {
    get,
    put,
  },
}))

describe('AdminVideosView', () => {
  beforeEach(() => {
    get.mockReset()
    put.mockReset()
  })

  it('loads pending videos from the admin backend and renders them', async () => {
    get.mockResolvedValue({
      records: [
        {
          id: '1001',
          authorUid: '2002',
          title: '待审核视频',
          description: '视频简介',
          coverUrl: 'https://example.com/cover.png',
          videoUrl: 'https://example.com/video.mp4',
          duration: 125,
          createTime: '2026-04-12T10:00:00',
          nickname: 'huangnv',
        },
      ],
      nextCursor: null,
      hasMore: false,
    })

    const wrapper = mount(AdminVideosView)
    await flushPromises()

    expect(get).toHaveBeenCalledWith('/admin/videos/pending')
    expect(wrapper.text()).toContain('待审核视频')
    expect(wrapper.text()).toContain('huangnv')
  })

  it('reviews a pending video through the admin backend', async () => {
    get.mockResolvedValueOnce({
      records: [
        {
          id: '1001',
          authorUid: '2002',
          title: '待审核视频',
          description: '视频简介',
          coverUrl: 'https://example.com/cover.png',
          videoUrl: 'https://example.com/video.mp4',
          duration: 125,
          createTime: '2026-04-12T10:00:00',
          nickname: 'huangnv',
        },
      ],
      nextCursor: null,
      hasMore: false,
    })
    get.mockResolvedValueOnce({
      records: [],
      nextCursor: null,
      hasMore: false,
    })
    put.mockResolvedValue({})

    const wrapper = mount(AdminVideosView)
    await flushPromises()

    const buttons = wrapper.findAll('button')
    await buttons.find((button) => button.text() === '查看审核')!.trigger('click')
    await flushPromises()
    await wrapper.get('button.approve-button').trigger('click')
    await flushPromises()

    expect(put).toHaveBeenCalledWith('/admin/videos/1001/status', { status: 0 })
    expect(get).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('当前没有待审核视频')
  })

  it('switches to the published tab and loads the published endpoint', async () => {
    get.mockResolvedValueOnce({
      records: [],
      nextCursor: null,
      hasMore: false,
    })
    get.mockResolvedValueOnce({
      records: [
        {
          id: '3003',
          authorUid: '4004',
          title: '已上架视频',
          description: '公开可见',
          coverUrl: 'https://example.com/published-cover.png',
          videoUrl: 'https://example.com/published-video.mp4',
          duration: 98,
          createTime: '2026-04-12T11:00:00',
          nickname: 'admin-user',
        },
      ],
      nextCursor: null,
      hasMore: false,
    })

    const wrapper = mount(AdminVideosView)
    await flushPromises()

    await wrapper.get('button[data-tab="published"]').trigger('click')
    await flushPromises()

    expect(get).toHaveBeenLastCalledWith('/admin/videos/published')
    expect(wrapper.text()).toContain('已上架视频')
    expect(wrapper.text()).toContain('当前展示已上架视频')
    expect(wrapper.find('button.approve-button').exists()).toBe(false)
    expect(wrapper.find('button.reject-button').exists()).toBe(false)
  })
})
