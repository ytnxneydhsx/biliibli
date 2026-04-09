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

vi.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => router,
}))

vi.mock('../../../lib/api', () => ({
  api: {
    get: (...args: unknown[]) => apiGet(...args),
    post: vi.fn(),
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
})
