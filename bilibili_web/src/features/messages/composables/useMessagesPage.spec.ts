import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, nextTick } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { authState } from '../../../lib/auth'
import { useMessagesPage } from './useMessagesPage'

const route = {
  query: {} as Record<string, unknown>,
}

const router = {
  replace: vi.fn(async ({ query }: { query?: Record<string, unknown> }) => {
    route.query = query || {}
  }),
}

const apiGet = vi.fn()
const apiPost = vi.fn()
const apiPut = vi.fn()
const apiDelete = vi.fn()

vi.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => router,
}))

vi.mock('../../../lib/api', () => ({
  api: {
    get: (...args: unknown[]) => apiGet(...args),
    post: (...args: unknown[]) => apiPost(...args),
    put: (...args: unknown[]) => apiPut(...args),
    delete: (...args: unknown[]) => apiDelete(...args),
  },
}))

class FakeWebSocket {
  static OPEN = 1
  static CLOSED = 3
  static instances: FakeWebSocket[] = []
  readyState = 1
  send = vi.fn()
  constructor() {
    FakeWebSocket.instances.push(this)
  }
  close = vi.fn(() => {
    this.readyState = FakeWebSocket.CLOSED
  })
  addEventListener = vi.fn()
}

describe('useMessagesPage group sending', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    FakeWebSocket.instances = []
    route.query = { groupId: '99' }
    authState.token = 'token'
    authState.uid = '7'
    authState.username = 'alice'
    authState.profile = {
      uid: '7',
      nickname: '小桃',
      avatar: '',
      sign: 'hi',
      followerCount: 1,
      followingCount: 1,
    }

    apiGet.mockImplementation(async (url: string) => {
      if (url === '/me/im/conversations') {
        return { records: [] }
      }
      if (url === '/me/im/conversations/groups') {
        return {
          records: [
            {
              groupId: '99',
              conversationId: 'g_99',
              groupName: '摸鱼群',
              lastMessage: '最近消息',
              lastMessageTime: '2026-04-09T10:00:00Z',
              unreadCount: 0,
            },
          ],
        }
      }
      if (url === '/me/im/groups/99') {
        return {
          groupId: '99',
          groupName: '摸鱼群',
          groupAvatar: '',
          memberCount: 5,
          ownerUserId: '7',
          isAllMuted: 0,
        }
      }
      if (url === '/me/im/groups/99/members') {
        return {
          records: [
            { userId: '7', role: 1, status: 1, isMuted: 0 },
            { userId: '8', role: 3, status: 1, isMuted: 0 },
          ],
        }
      }
      if (url === '/me/im/groups/99/messages/history') {
        return {
          records: [],
          hasMore: false,
          nextBeforeServerMessageId: null,
        }
      }
      return { records: [] }
    })

    apiPost.mockResolvedValue(undefined)
    apiPut.mockResolvedValue(undefined)
    apiDelete.mockResolvedValue(undefined)

    vi.stubGlobal('WebSocket', FakeWebSocket as unknown as typeof WebSocket)
  })

  it('allows sending messages to group chats through websocket with group params', async () => {
    let page!: ReturnType<typeof useMessagesPage>

    const Harness = defineComponent({
      setup() {
        page = useMessagesPage()
        return () => null
      },
    })

    mount(Harness)
    await flushPromises()
    await nextTick()

    expect(page.activeTargetType.value).toBe('group')
    expect(page.activeGroupId.value).toBe('99')

    page.setMessageDraft('大家好')
    await nextTick()

    expect(page.canSend.value).toBe(true)

    await page.sendMessage()

    expect(FakeWebSocket.instances).toHaveLength(1)
    expect(FakeWebSocket.instances[0].send).toHaveBeenCalledTimes(1)
    expect(FakeWebSocket.instances[0].send).toHaveBeenCalledWith(
      expect.stringContaining('"receiverId":"99"'),
    )
    expect(FakeWebSocket.instances[0].send).toHaveBeenCalledWith(
      expect.stringContaining('"conversationType":2'),
    )
  })

  it('exposes group settings controls for managers and updates the group name', async () => {
    let page!: ReturnType<typeof useMessagesPage>

    const Harness = defineComponent({
      setup() {
        page = useMessagesPage()
        return () => null
      },
    })

    mount(Harness)
    await flushPromises()
    await nextTick()

    expect(page.canManageActiveGroup.value).toBe(true)

    page.openGroupSettings()
    await flushPromises()

    expect(page.groupSettingsVisible.value).toBe(true)
    expect(page.activeGroupMembers.value).toHaveLength(2)

    page.setGroupNameDraft('新的群聊名字')
    await page.saveGroupName()

    expect(apiPut).toHaveBeenCalledWith('/me/im/groups/99/name', {
      groupName: '新的群聊名字',
    })
    expect(page.activeGroupProfile.value?.groupName).toBe('新的群聊名字')
  })

  it('manages group moderation actions through the existing group APIs', async () => {
    let page!: ReturnType<typeof useMessagesPage>

    const Harness = defineComponent({
      setup() {
        page = useMessagesPage()
        return () => null
      },
    })

    mount(Harness)
    await flushPromises()
    await nextTick()

    await page.toggleGroupMuteStatus()
    expect(apiPut).toHaveBeenCalledWith('/me/im/groups/99/mute', { isMuted: 1 })

    await page.toggleMemberMute('8', 0)
    expect(apiPut).toHaveBeenCalledWith('/me/im/groups/99/members/8/mute', { isMuted: 1 })

    page.setInviteGroupMemberUid('12')
    await page.inviteGroupMember()
    expect(apiPost).toHaveBeenCalledWith('/me/im/groups/99/members', { targetUserId: 12 })

    await page.kickGroupMember('8')
    expect(apiDelete).toHaveBeenCalledWith('/me/im/groups/99/members/8')
  })

  it('clears the local unread badge when opening an unread group conversation', async () => {
    route.query = { groupId: '2' }

    apiGet.mockImplementation(async (url: string) => {
      if (url === '/me/im/conversations') {
        return { records: [] }
      }
      if (url === '/me/im/conversations/groups') {
        return {
          records: [
            {
              groupId: '2',
              conversationId: 'g_2',
              groupName: '二号群',
              lastMessage: '还有未读',
              lastMessageTime: '2026-04-09T10:30:00Z',
              unreadCount: 3,
            },
          ],
        }
      }
      if (url === '/me/im/groups/2') {
        return {
          groupId: '2',
          groupName: '二号群',
          groupAvatar: '',
          memberCount: 4,
          ownerUserId: '7',
          isAllMuted: 0,
        }
      }
      if (url === '/me/im/groups/2/members') {
        return {
          records: [{ userId: '7', role: 1, status: 1, isMuted: 0 }],
        }
      }
      if (url === '/me/im/groups/2/messages/history') {
        return {
          records: [],
          hasMore: false,
          nextBeforeServerMessageId: null,
        }
      }
      return { records: [] }
    })

    let page!: ReturnType<typeof useMessagesPage>

    const Harness = defineComponent({
      setup() {
        page = useMessagesPage()
        return () => null
      },
    })

    mount(Harness)
    await flushPromises()
    await nextTick()

    expect(page.sortedGroupConversations.value[0].groupId).toBe('2')
    expect(page.activeGroupId.value).toBe('2')
    expect(page.sortedGroupConversations.value[0].unreadCount).toBe(0)
  })
})
