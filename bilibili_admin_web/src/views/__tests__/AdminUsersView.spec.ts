import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AdminUsersView from '../AdminUsersView.vue'

const { get, post, remove } = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  remove: vi.fn(),
}))

vi.mock('../../lib/api', () => ({
  api: {
    get,
    post,
    delete: remove,
  },
}))

describe('AdminUsersView', () => {
  beforeEach(() => {
    get.mockReset()
    post.mockReset()
    remove.mockReset()
  })

  it('loads users from the admin backend and renders their permission state', async () => {
    get.mockResolvedValue({
      records: [
        {
          uid: '1001',
          username: 'huangnv',
          roleCode: 1,
          status: 0,
          nickname: '黄女',
          avatar: 'https://example.com/avatar.png',
          sign: 'admin',
          likeEnabled: true,
          commentEnabled: true,
          imMessageSendEnabled: true,
          videoUploadEnabled: true,
          profileEditEnabled: true,
          videoBusinessBanned: false,
        },
      ],
      total: 1,
      pageNo: 1,
      pageSize: 10,
      totalPages: 1,
    })

    const wrapper = mount(AdminUsersView)
    await flushPromises()

    expect(get).toHaveBeenCalledWith('/admin/users', { pageNo: 1, pageSize: 10 })
    expect(wrapper.text()).toContain('huangnv')
    expect(wrapper.text()).toContain('黄女')
    expect(wrapper.text()).toContain('正常')
    expect(wrapper.text()).toContain('未封禁')
  })

  it('bans and unbans a user through the admin backend', async () => {
    get.mockResolvedValue({
      records: [
        {
          uid: '1001',
          username: 'huangnv',
          roleCode: 1,
          status: 0,
          nickname: '黄女',
          avatar: 'https://example.com/avatar.png',
          sign: 'admin',
          likeEnabled: true,
          commentEnabled: true,
          imMessageSendEnabled: true,
          videoUploadEnabled: true,
          profileEditEnabled: true,
          videoBusinessBanned: false,
        },
        {
          uid: '1002',
          username: 'guest',
          roleCode: 0,
          status: 0,
          nickname: '游客',
          avatar: '',
          sign: '',
          likeEnabled: false,
          commentEnabled: false,
          imMessageSendEnabled: true,
          videoUploadEnabled: false,
          profileEditEnabled: true,
          videoBusinessBanned: true,
        },
      ],
      total: 2,
      pageNo: 1,
      pageSize: 10,
      totalPages: 1,
    })
    post.mockResolvedValue({})
    remove.mockResolvedValue({})

    const wrapper = mount(AdminUsersView)
    await flushPromises()

    await wrapper.get('button[data-user-action="ban"]').trigger('click')
    await flushPromises()
    expect(post).toHaveBeenCalledWith('/admin/users/1001/video-business-ban')

    await wrapper.get('button[data-user-action="unban"]').trigger('click')
    await flushPromises()
    expect(remove).toHaveBeenCalledWith('/admin/users/1002/video-business-ban')
  })
})
