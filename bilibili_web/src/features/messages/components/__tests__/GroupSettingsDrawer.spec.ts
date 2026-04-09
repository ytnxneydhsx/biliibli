import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import GroupSettingsDrawer from '../GroupSettingsDrawer.vue'

describe('GroupSettingsDrawer', () => {
  const baseProps = {
    visible: true,
    activeTab: 'profile' as const,
    groupNameDraft: '摸鱼群',
    groupAvatar: '',
    groupNameSaving: false,
    groupAvatarUploading: false,
    profileError: '',
    members: [
      { userId: '7', role: 1, status: 1, isMuted: 0 },
      { userId: '8', role: 3, status: 1, isMuted: 0 },
    ],
    currentUid: '7',
    inviteGroupMemberUid: '',
    inviteGroupMemberLoading: false,
    inviteGroupMemberError: '',
    groupMembersLoading: false,
    groupMembersError: '',
    groupMuteUpdating: false,
    isAllMuted: 0,
    moderationError: '',
    actionTargetUserId: '',
    resolvePeerName: (uid: string) => `用户 ${uid}`,
    resolvePeerAvatar: () => '',
    resolveGroupMemberRole: (role?: number) => (role === 1 ? '群主' : '成员'),
    resolveGroupMemberStatus: () => '正常',
  }

  it('renders the drawer tabs and emits profile actions', async () => {
    const wrapper = mount(GroupSettingsDrawer, {
      props: baseProps,
    })

    expect(wrapper.text()).toContain('群资料')
    expect(wrapper.text()).toContain('成员管理')
    expect(wrapper.text()).toContain('群管理')

    await wrapper.get('input[type="text"]').setValue('今晚不睡觉')
    expect(wrapper.emitted('update:groupNameDraft')?.[0]).toEqual(['今晚不睡觉'])

    await wrapper.get('form').trigger('submit.prevent')
    expect(wrapper.emitted('saveGroupName')).toHaveLength(1)
  })

  it('shows member operations and moderation controls in their tabs', async () => {
    const wrapper = mount(GroupSettingsDrawer, {
      props: {
        ...baseProps,
        activeTab: 'members' as const,
      },
    })

    expect(wrapper.text()).toContain('用户 8')
    expect(wrapper.text()).toContain('邀请成员')

    await wrapper.get('.member-action-button').trigger('click')
    expect(wrapper.emitted('toggleMemberMute')?.[0]).toEqual(['8', 0])

    await wrapper.get('.member-kick-button').trigger('click')
    expect(wrapper.emitted('kickMember')?.[0]).toEqual(['8'])

    await wrapper.findAll('.drawer-tab')[2].trigger('click')
    expect(wrapper.emitted('switchTab')?.[0]).toEqual(['moderation'])
  })
})
