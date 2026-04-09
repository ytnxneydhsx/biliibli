import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import HomeView from '../HomeView.vue'
import { api } from '../../lib/api'

vi.mock('../../lib/api', () => ({
  api: {
    get: vi.fn(),
  },
}))

const mockedGet = vi.mocked(api.get)

describe('HomeView', () => {
  beforeEach(() => {
    mockedGet.mockReset()
  })

  it('renders a featured hero from the first real video and a real ranking list', async () => {
    mockedGet
      .mockResolvedValueOnce({
        records: [
          {
            id: '101',
            authorUid: '7',
            title: '春日第一条视频',
            coverUrl: 'https://example.com/cover-1.jpg',
            viewCount: 1200,
            duration: 120,
            createTime: '2026-04-09 09:00:00',
            nickname: '小明',
          },
          {
            id: '102',
            authorUid: '8',
            title: '第二条公开视频',
            coverUrl: 'https://example.com/cover-2.jpg',
            viewCount: 800,
            duration: 95,
            createTime: '2026-04-09 10:00:00',
            nickname: '小红',
          },
        ],
      })
      .mockResolvedValueOnce({
        records: [
          {
            rank: 1,
            score: 99,
            id: '301',
            authorUid: '15',
            title: '热榜第一',
            coverUrl: 'https://example.com/rank-1.jpg',
            viewCount: 9900,
            duration: 66,
            createTime: '2026-04-08 12:00:00',
            nickname: '榜单作者',
          },
        ],
      })

    const wrapper = mount(HomeView, {
      global: {
        stubs: {
          RouterLink: RouterLinkStub,
          VideoCard: true,
        },
      },
    })

    await flushPromises()

    expect(wrapper.find('.featured-hero').exists()).toBe(true)
    expect(wrapper.text()).toContain('春日第一条视频')
    expect(wrapper.find('[data-testid="featured-watch-link"]').attributes('to')).toBe('/video/101')
    expect(wrapper.findAll('.hero-rank-item')).toHaveLength(1)
    expect(wrapper.text()).toContain('热榜第一')
    expect(mockedGet).toHaveBeenNthCalledWith(1, '/videos', { pageNo: 1, pageSize: 12 })
    expect(mockedGet).toHaveBeenNthCalledWith(2, '/videos/rank', { pageNo: 1, pageSize: 8 })
  })

  it('shows an honest empty state when videos are empty', async () => {
    mockedGet.mockResolvedValueOnce({ records: [] }).mockResolvedValueOnce({ records: [] })

    const wrapper = mount(HomeView, {
      global: {
        stubs: {
          RouterLink: RouterLinkStub,
          VideoCard: true,
        },
      },
    })

    await flushPromises()

    expect(wrapper.find('.featured-hero').exists()).toBe(false)
    expect(wrapper.text()).toContain('当前还没有可展示的视频')
  })
})
