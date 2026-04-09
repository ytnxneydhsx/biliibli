import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import MessagesSidebar from '../MessagesSidebar.vue'

describe('MessagesSidebar', () => {
  const baseProps = {
    currentProfile: null,
    currentUid: '7',
    currentUsername: 'alice',
    connectionState: 'live' as const,
    connectionLabel: '已连接',
    activeTab: 'single' as const,
    singleConversations: [
      {
        peerUid: '8',
        conversationId: 'single_7_8',
        lastMessage: '你好呀',
        lastMessageTime: '刚刚',
        lastMessageEpoch: 100,
        unreadCount: 2,
        hasMoreHistory: false,
        nextBeforeServerMessageId: '',
        historyLoaded: true,
      },
    ],
    groupConversations: [
      {
        groupId: '99',
        conversationId: 'g_99',
        groupName: '摸鱼小组',
        lastMessage: '今晚开黑',
        lastMessageTime: '昨天',
        unreadCount: 3,
      },
    ],
    activePeerUid: '8',
    activeGroupId: '',
    resolvePeerName: (peerUid: string) => `用户 ${peerUid}`,
    resolvePeerAvatar: () => '',
    resolveGroupName: (groupId: string) => `群 ${groupId}`,
    resolveGroupAvatar: () => '',
  }

  it('shows single and group tabs and only renders the active tab list', () => {
    const wrapper = mount(MessagesSidebar, {
      props: baseProps,
    })

    expect(wrapper.text()).toContain('私人聊天')
    expect(wrapper.text()).toContain('群聊天')
    expect(wrapper.text()).toContain('用户 8')
    expect(wrapper.text()).not.toContain('群 99')
  })

  it('emits tab switches and opens group conversations from the group tab', async () => {
    const wrapper = mount(MessagesSidebar, {
      props: {
        ...baseProps,
        activeTab: 'group' as const,
        activePeerUid: '',
        activeGroupId: '99',
      },
    })

    const tabButtons = wrapper.findAll('.sidebar-tab')
    await tabButtons[0].trigger('click')
    expect(wrapper.emitted('switchTab')?.[0]).toEqual(['single'])

    const groupItem = wrapper.findAll('.conversation-item')[0]
    expect(wrapper.text()).toContain('群 99')
    await groupItem.trigger('click')

    expect(wrapper.emitted('openGroup')?.[0]).toEqual(['99'])
  })
})
