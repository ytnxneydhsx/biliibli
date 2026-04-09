import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../../../lib/api'
import { authState } from '../../../lib/auth'
import type { UserProfileVO } from '../../../types'
import type {
  AcceptedPayload,
  ActiveTargetType,
  ConnectionState,
  ConversationItem,
  ConversationUpdatedPayload,
  GroupConversationWindowListVO,
  GroupConversationWindowVO,
  ConversationWindowListVO,
  ConversationWindowVO,
  DraftImageItem,
  EventLogItem,
  GroupConversationUpdatedPayload,
  GroupMemberItem,
  GroupMemberListVO,
  GroupProfileVO,
  GroupSettingsTab,
  MessageContent,
  MessageHistoryVO,
  MessageItem,
  MessagePushPayload,
  MessageVO,
  WsPacket,
} from '../types'

const MESSAGE_TYPE_TEXT = 1
const MESSAGE_TYPE_IMAGE = 2
const MESSAGE_TYPE_RICH = 3

type GroupHistoryState = {
  hasMoreHistory: boolean
  nextBeforeServerMessageId: string
  historyLoaded: boolean
}

export function useMessagesPage() {
  const route = useRoute()
  const router = useRouter()

  const socket = ref<WebSocket | null>(null)
  const heartbeatTimer = ref<number | null>(null)
  const connectionState = ref<ConnectionState>('idle')
  const eventLogs = ref<EventLogItem[]>([])
  const conversations = ref<Record<string, ConversationItem>>({})
  const groupWindows = ref<Record<string, GroupConversationWindowVO>>({})
  const messagesByStream = ref<Record<string, MessageItem[]>>({})
  const pendingMessages = ref<Record<string, MessageItem>>({})
  const peerProfiles = ref<Record<string, UserProfileVO>>({})
  const groupProfiles = ref<Record<string, GroupProfileVO>>({})
  const groupMembersByGroup = ref<Record<string, GroupMemberItem[]>>({})
  const groupHistoryStates = ref<Record<string, GroupHistoryState>>({})
  const loadingPeer = ref(false)
  const loadingGroup = ref(false)
  const loadingConversations = ref(false)
  const loadingHistory = ref(false)
  const readingConversation = ref(false)
  const uploadError = ref('')
  const groupSettingsVisible = ref(false)
  const groupSettingsTab = ref<GroupSettingsTab>('profile')
  const groupNameDraft = ref('')
  const groupNameSaving = ref(false)
  const groupAvatarUploading = ref(false)
  const profileError = ref('')
  const inviteGroupMemberUid = ref('')
  const inviteGroupMemberLoading = ref(false)
  const inviteGroupMemberError = ref('')
  const groupMembersLoading = ref(false)
  const groupMembersError = ref('')
  const groupMuteUpdating = ref(false)
  const moderationError = ref('')
  const actionTargetUserId = ref('')
  const messageDraft = ref('')
  const draftImages = ref<DraftImageItem[]>([])
  const messageStream = ref<HTMLDivElement | null>(null)
  const sidebarTab = ref<ActiveTargetType>('single')
  const activeTargetType = ref<ActiveTargetType>('single')
  const activePeerUid = ref('')
  const activeGroupId = ref('')

  const currentUid = computed(() => authState.uid || '')
  const currentToken = computed(() => authState.token || '')
  const currentProfile = computed(() => authState.profile)
  const currentUsername = computed(() => authState.username || '')
  const peerUidFromRoute = computed(() => {
    const value = String(route.query.peerUid ?? '').trim()
    return /^\d+$/.test(value) ? value : ''
  })
  const groupIdFromRoute = computed(() => {
    const value = String(route.query.groupId ?? '').trim()
    return /^\d+$/.test(value) ? value : ''
  })

  const connectionLabel = computed(() => {
    if (connectionState.value === 'live') return '已连接'
    if (connectionState.value === 'connecting') return '连接中'
    if (connectionState.value === 'error') return '连接异常'
    return '未连接'
  })

  const sortedSingleConversations = computed(() =>
    Object.values(conversations.value).sort((left, right) => right.lastMessageEpoch - left.lastMessageEpoch),
  )

  const sortedGroupConversations = computed(() =>
    Object.values(groupWindows.value).sort(
      (left, right) => toEpoch(right.lastMessageTime) - toEpoch(left.lastMessageTime),
    ),
  )

  const activeStreamKey = computed(() => {
    if (activeTargetType.value === 'group' && activeGroupId.value) {
      return buildStreamKey('group', activeGroupId.value)
    }
    if (activeTargetType.value === 'single' && activePeerUid.value) {
      return buildStreamKey('single', activePeerUid.value)
    }
    return ''
  })

  const activeMessages = computed(() => {
    if (!activeStreamKey.value) return []
    return messagesByStream.value[activeStreamKey.value] || []
  })

  const activeConversation = computed(() => {
    if (activeTargetType.value !== 'single' || !activePeerUid.value) return null
    return conversations.value[activePeerUid.value] || null
  })

  const activePeerProfile = computed(() => {
    if (activeTargetType.value !== 'single' || !activePeerUid.value) return null
    return peerProfiles.value[activePeerUid.value] || null
  })

  const activeGroupProfile = computed(() => {
    if (activeTargetType.value !== 'group' || !activeGroupId.value) return null
    return groupProfiles.value[activeGroupId.value] || null
  })

  const activeGroupMembers = computed(() => {
    if (activeTargetType.value !== 'group' || !activeGroupId.value) return []
    return groupMembersByGroup.value[activeGroupId.value] || []
  })

  const activeGroupRole = computed(() => {
    if (activeTargetType.value !== 'group' || !activeGroupId.value || !currentUid.value) {
      return 0
    }
    if (String(activeGroupProfile.value?.ownerUserId || '') === currentUid.value) {
      return 1
    }
    const membership = activeGroupMembers.value.find(
      (member) => String(member.userId || '') === currentUid.value,
    )
    return Number(membership?.role || 0)
  })

  const canManageActiveGroup = computed(() => activeGroupRole.value === 1 || activeGroupRole.value === 2)

  const activeHasMoreHistory = computed(() => {
    if (activeTargetType.value === 'group') {
      if (!activeGroupId.value) return false
      return Boolean(groupHistoryStates.value[activeGroupId.value]?.hasMoreHistory)
    }
    return Boolean(activeConversation.value?.hasMoreHistory)
  })

  const hasUploadingImages = computed(() => draftImages.value.some((item) => item.uploading))
  const hasFailedImages = computed(() => draftImages.value.some((item) => !!item.error))
  const canSend = computed(() => {
    const targetAvailable =
      (activeTargetType.value === 'single' && !!activePeerUid.value) ||
      (activeTargetType.value === 'group' && !!activeGroupId.value)
    if (!targetAvailable) return false
    if (hasUploadingImages.value) return false
    return !!messageDraft.value.trim() || draftImages.value.some((item) => !!item.uploadedUrl)
  })

  const activeConversationTitle = computed(() => {
    if (activeTargetType.value === 'group') {
      if (!activeGroupId.value) return '还没有打开任何会话'
      return activeGroupProfile.value?.groupName || `群聊 ${activeGroupId.value}`
    }
    if (!activePeerUid.value) return '还没有打开任何会话'
    if (activePeerProfile.value?.nickname) return `与 ${activePeerProfile.value.nickname} 的对话`
    return `与 UID ${activePeerUid.value} 的对话`
  })

  const activeConversationSubtitle = computed(() => {
    if (activeTargetType.value === 'group') {
      if (!activeGroupId.value) {
        return '先从私信入口进入单聊，或者通过群入口带上 groupId 打开群聊模式。'
      }
      if (loadingGroup.value && !activeGroupProfile.value) {
        return '正在补全群资料。'
      }
      if (loadingHistory.value && !activeMessages.value.length) {
        return '正在加载群最近消息。'
      }
      if (activeGroupProfile.value?.memberCount) {
        return `${activeGroupProfile.value.memberCount} 位成员，当前主区已支持群历史与群实时接收。`
      }
      return '群聊模式当前先接入历史消息和实时接收，发送入口稍后补齐。'
    }
    if (!activePeerUid.value) {
      return '从用户主页点击“私信”进入，或者等待实时窗口推送出现在这里。'
    }
    if (loadingPeer.value && !activePeerProfile.value) {
      return '正在补全对方资料。'
    }
    if (loadingHistory.value && !activeMessages.value.length) {
      return '正在加载最近消息。'
    }
    return '支持历史消息、实时收发、窗口清未读和聊天图片上传。'
  })

  const wsUrl = computed(() => {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    return `${protocol}//${window.location.host}/ws/im`
  })

  watch(
    [groupIdFromRoute, peerUidFromRoute],
    async ([groupId, peerUid]) => {
      if (groupId) {
        sidebarTab.value = 'group'
        await activateGroup(groupId)
        return
      }
      if (peerUid) {
        sidebarTab.value = 'single'
        await activateConversation(peerUid)
        return
      }
      activeTargetType.value = sidebarTab.value
      activePeerUid.value = ''
      activeGroupId.value = ''
    },
    { immediate: true },
  )

  watch(
    currentToken,
    async (token) => {
      if (!token) {
        disconnectSocket()
        return
      }
      await loadConversationWindows()
      if (!socket.value || socket.value.readyState === WebSocket.CLOSED) {
        connectSocket()
      }
    },
    { immediate: true },
  )

  onMounted(async () => {
    if (currentToken.value) {
      await loadConversationWindows()
    }
  })

  onBeforeUnmount(() => {
    disconnectSocket()
    revokeDraftPreviews()
  })

  async function activateConversation(peerUid: string) {
    ensureConversation(peerUid)
    activeTargetType.value = 'single'
    activePeerUid.value = peerUid
    activeGroupId.value = ''
    groupSettingsVisible.value = false
    await Promise.all([
      loadPeerProfile(peerUid),
      ensureHistoryLoaded(peerUid),
    ])
    await markConversationRead(peerUid)
    await nextTick()
    scrollStreamToBottom()
  }

  async function activateGroup(groupId: string) {
    ensureGroupHistoryState(groupId)
    activeTargetType.value = 'group'
    activeGroupId.value = groupId
    activePeerUid.value = ''
    upsertGroupWindow(groupId, { unreadCount: 0 })
    await Promise.all([
      loadGroupProfile(groupId),
      ensureGroupHistoryLoaded(groupId),
      loadGroupMembers(groupId),
    ])
    groupNameDraft.value = groupProfiles.value[groupId]?.groupName || ''
    await nextTick()
    scrollStreamToBottom()
  }

  async function loadPeerProfile(peerUid: string) {
    if (!peerUid || peerProfiles.value[peerUid]) {
      return
    }
    loadingPeer.value = true
    try {
      const profile = await api.get<UserProfileVO>(`/users/${peerUid}`)
      peerProfiles.value = {
        ...peerProfiles.value,
        [peerUid]: profile,
      }
    } catch {
      // keep page usable even if profile fetch fails
    } finally {
      loadingPeer.value = false
    }
  }

  async function loadGroupProfile(groupId: string) {
    if (!groupId || groupProfiles.value[groupId]) {
      return
    }
    loadingGroup.value = true
    try {
      const profile = await api.get<GroupProfileVO>(`/me/im/groups/${groupId}`)
      groupProfiles.value = {
        ...groupProfiles.value,
        [groupId]: profile,
      }
    } catch {
      // keep page usable even if group profile fetch fails
    } finally {
      loadingGroup.value = false
    }
  }

  async function loadGroupMembers(groupId = activeGroupId.value) {
    if (!groupId) {
      return
    }
    groupMembersLoading.value = true
    groupMembersError.value = ''
    try {
      const payload = await api.get<GroupMemberListVO>(`/me/im/groups/${groupId}/members`)
      groupMembersByGroup.value = {
        ...groupMembersByGroup.value,
        [groupId]: payload.records || [],
      }
      const peerUids = (payload.records || [])
        .map((item) => String(item.userId || ''))
        .filter((uid) => uid && uid !== currentUid.value)
      await Promise.all(peerUids.map((uid) => loadPeerProfile(uid)))
    } catch (error) {
      groupMembersError.value = (error as Error).message || '群成员加载失败'
    } finally {
      groupMembersLoading.value = false
    }
  }

  function openGroupSettings() {
    if (!activeGroupId.value || !canManageActiveGroup.value) {
      return
    }
    groupSettingsVisible.value = true
    profileError.value = ''
    moderationError.value = ''
    inviteGroupMemberError.value = ''
    groupMembersError.value = ''
    groupSettingsTab.value = 'profile'
    groupNameDraft.value = activeGroupProfile.value?.groupName || ''
    void loadGroupMembers()
  }

  function closeGroupSettings() {
    groupSettingsVisible.value = false
  }

  function setGroupSettingsTab(tab: GroupSettingsTab) {
    groupSettingsTab.value = tab
    if (tab === 'members') {
      void loadGroupMembers()
    }
  }

  function setGroupNameDraft(value: string) {
    groupNameDraft.value = value
  }

  async function saveGroupName() {
    if (!activeGroupId.value) {
      return
    }
    profileError.value = ''
    groupNameSaving.value = true
    const nextName = groupNameDraft.value.trim()
    try {
      const savedName = await api.put<string>(`/me/im/groups/${activeGroupId.value}/name`, {
        groupName: nextName,
      })
      const groupId = activeGroupId.value
      const resolvedName = String(savedName || nextName)
      groupNameDraft.value = resolvedName
      if (groupProfiles.value[groupId]) {
        groupProfiles.value = {
          ...groupProfiles.value,
          [groupId]: {
            ...groupProfiles.value[groupId],
            groupName: resolvedName,
          },
        }
      }
      if (groupWindows.value[groupId]) {
        upsertGroupWindow(groupId, { groupName: resolvedName })
      }
    } catch (error) {
      profileError.value = (error as Error).message || '群名称保存失败'
    } finally {
      groupNameSaving.value = false
    }
  }

  async function uploadGroupAvatar(files: File[]) {
    if (!activeGroupId.value || !files.length) {
      return
    }
    profileError.value = ''
    groupAvatarUploading.value = true
    try {
      const form = new FormData()
      form.append('file', files[0])
      const avatarUrl = await api.put<string>(`/me/im/groups/${activeGroupId.value}/avatar`, form, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      })
      const groupId = activeGroupId.value
      if (groupProfiles.value[groupId]) {
        groupProfiles.value = {
          ...groupProfiles.value,
          [groupId]: {
            ...groupProfiles.value[groupId],
            groupAvatar: avatarUrl,
          },
        }
      }
      if (groupWindows.value[groupId]) {
        upsertGroupWindow(groupId, { groupAvatar: avatarUrl })
      }
    } catch (error) {
      profileError.value = (error as Error).message || '群头像上传失败'
    } finally {
      groupAvatarUploading.value = false
    }
  }

  function setInviteGroupMemberUid(value: string) {
    inviteGroupMemberUid.value = value.replace(/[^\d]/g, '')
  }

  async function inviteGroupMember() {
    if (!activeGroupId.value || !inviteGroupMemberUid.value) {
      return
    }
    inviteGroupMemberError.value = ''
    inviteGroupMemberLoading.value = true
    try {
      await api.post<void>(`/me/im/groups/${activeGroupId.value}/members`, {
        targetUserId: Number(inviteGroupMemberUid.value),
      })
      inviteGroupMemberUid.value = ''
      await loadGroupMembers()
    } catch (error) {
      inviteGroupMemberError.value = (error as Error).message || '邀请成员失败'
    } finally {
      inviteGroupMemberLoading.value = false
    }
  }

  async function toggleGroupMuteStatus() {
    if (!activeGroupId.value || !activeGroupProfile.value) {
      return
    }
    moderationError.value = ''
    groupMuteUpdating.value = true
    const nextMuted = Number(activeGroupProfile.value.isAllMuted || 0) === 1 ? 0 : 1
    try {
      await api.put<void>(`/me/im/groups/${activeGroupId.value}/mute`, {
        isMuted: nextMuted,
      })
      const groupId = activeGroupId.value
      groupProfiles.value = {
        ...groupProfiles.value,
        [groupId]: {
          ...groupProfiles.value[groupId],
          isAllMuted: nextMuted,
        },
      }
      if (groupWindows.value[groupId]) {
        upsertGroupWindow(groupId, { isAllMuted: nextMuted })
      }
    } catch (error) {
      moderationError.value = (error as Error).message || '全员禁言设置失败'
    } finally {
      groupMuteUpdating.value = false
    }
  }

  async function toggleMemberMute(targetUserId: string, currentMuted: number) {
    if (!activeGroupId.value || !targetUserId) {
      return
    }
    moderationError.value = ''
    actionTargetUserId.value = targetUserId
    const nextMuted = Number(currentMuted || 0) === 1 ? 0 : 1
    try {
      await api.put<void>(`/me/im/groups/${activeGroupId.value}/members/${targetUserId}/mute`, {
        isMuted: nextMuted,
      })
      groupMembersByGroup.value = {
        ...groupMembersByGroup.value,
        [activeGroupId.value]: activeGroupMembers.value.map((member) =>
          String(member.userId || '') === targetUserId
            ? { ...member, isMuted: nextMuted }
            : member,
        ),
      }
    } catch (error) {
      moderationError.value = (error as Error).message || '成员禁言设置失败'
    } finally {
      actionTargetUserId.value = ''
    }
  }

  async function updateMemberRole(targetUserId: string, currentRole: number) {
    if (!activeGroupId.value || !targetUserId || activeGroupRole.value !== 1) {
      return
    }
    moderationError.value = ''
    actionTargetUserId.value = targetUserId
    const nextRole = Number(currentRole || 0) === 2 ? 3 : 2
    try {
      await api.put<void>(`/me/im/groups/${activeGroupId.value}/members/${targetUserId}/role`, {
        role: nextRole,
      })
      groupMembersByGroup.value = {
        ...groupMembersByGroup.value,
        [activeGroupId.value]: activeGroupMembers.value.map((member) =>
          String(member.userId || '') === targetUserId
            ? { ...member, role: nextRole }
            : member,
        ),
      }
    } catch (error) {
      moderationError.value = (error as Error).message || '成员角色更新失败'
    } finally {
      actionTargetUserId.value = ''
    }
  }

  async function kickGroupMember(targetUserId: string) {
    if (!activeGroupId.value || !targetUserId) {
      return
    }
    moderationError.value = ''
    actionTargetUserId.value = targetUserId
    try {
      await api.delete<void>(`/me/im/groups/${activeGroupId.value}/members/${targetUserId}`)
      groupMembersByGroup.value = {
        ...groupMembersByGroup.value,
        [activeGroupId.value]: activeGroupMembers.value.filter(
          (member) => String(member.userId || '') !== targetUserId,
        ),
      }
      const nextCount = Math.max(0, Number(activeGroupProfile.value?.memberCount || 0) - 1)
      if (groupProfiles.value[activeGroupId.value]) {
        groupProfiles.value = {
          ...groupProfiles.value,
          [activeGroupId.value]: {
            ...groupProfiles.value[activeGroupId.value],
            memberCount: nextCount,
          },
        }
      }
      if (groupWindows.value[activeGroupId.value]) {
        upsertGroupWindow(activeGroupId.value, { memberCount: nextCount })
      }
    } catch (error) {
      moderationError.value = (error as Error).message || '移出成员失败'
    } finally {
      actionTargetUserId.value = ''
    }
  }

  async function ensureHistoryLoaded(peerUid: string) {
    const conversation = conversations.value[peerUid]
    if (conversation?.historyLoaded) {
      return
    }
    await loadHistoryPage(peerUid, false)
  }

  async function ensureGroupHistoryLoaded(groupId: string) {
    const state = groupHistoryStates.value[groupId]
    if (state?.historyLoaded) {
      return
    }
    await loadGroupHistoryPage(groupId, false)
  }

  async function loadConversationWindows() {
    if (!currentToken.value || loadingConversations.value) {
      return
    }

    loadingConversations.value = true
    try {
      const [singlePayload, groupPayload] = await Promise.all([
        api.get<ConversationWindowListVO>('/me/im/conversations'),
        api.get<GroupConversationWindowListVO>('/me/im/conversations/groups'),
      ])
      const records = singlePayload.records || []
      const loadedPeerUids: string[] = []
      for (const record of records) {
        const peerUid = applyConversationWindow(record)
        if (peerUid) {
          loadedPeerUids.push(peerUid)
        }
      }
      for (const record of groupPayload.records || []) {
        applyGroupConversationWindow(record)
      }
      await Promise.all(loadedPeerUids.map((peerUid) => loadPeerProfile(peerUid)))
      if (
        !groupIdFromRoute.value &&
        !peerUidFromRoute.value &&
        !activeGroupId.value &&
        !activePeerUid.value &&
        sortedSingleConversations.value.length > 0
      ) {
        openConversation(sortedSingleConversations.value[0].peerUid)
      }
    } catch (error) {
      pushEvent('error', (error as Error).message || '会话窗口加载失败')
    } finally {
      loadingConversations.value = false
    }
  }

  async function loadHistoryPage(peerUid: string, appendOlder: boolean) {
    if (!peerUid) {
      return
    }
    const conversation = conversations.value[peerUid]
    if (appendOlder && (!conversation || !conversation.hasMoreHistory || !conversation.nextBeforeServerMessageId)) {
      return
    }

    loadingHistory.value = true
    try {
      const beforeServerMessageId = appendOlder ? conversation?.nextBeforeServerMessageId || undefined : undefined
      const history = await api.get<MessageHistoryVO>('/me/im/messages/history', {
        peerUid,
        beforeServerMessageId,
      })
      const mapped = (history.records || []).map((record) => toSingleMessageItem(record, peerUid))
      mergeMessages(buildStreamKey('single', peerUid), mapped)
      upsertConversation(peerUid, {
        hasMoreHistory: Boolean(history.hasMore),
        nextBeforeServerMessageId: String(history.nextBeforeServerMessageId || ''),
        historyLoaded: true,
      })
    } catch (error) {
      pushEvent('error', (error as Error).message || '历史消息加载失败')
    } finally {
      loadingHistory.value = false
    }
  }

  async function loadGroupHistoryPage(groupId: string, appendOlder: boolean) {
    if (!groupId) {
      return
    }
    ensureGroupHistoryState(groupId)
    const state = groupHistoryStates.value[groupId]
    if (appendOlder && (!state || !state.hasMoreHistory || !state.nextBeforeServerMessageId)) {
      return
    }

    loadingHistory.value = true
    try {
      const beforeServerMessageId = appendOlder ? state?.nextBeforeServerMessageId || undefined : undefined
      const history = await api.get<MessageHistoryVO>(`/me/im/groups/${groupId}/messages/history`, {
        beforeServerMessageId,
      })
      const senderIds = Array.from(
        new Set(
          (history.records || [])
            .map((record) => String(record.senderId || ''))
            .filter((senderId) => !!senderId && senderId !== currentUid.value),
        ),
      )
      await Promise.all(senderIds.map((senderId) => loadPeerProfile(senderId)))
      const mapped = (history.records || []).map((record) => toGroupMessageItem(record))
      mergeMessages(buildStreamKey('group', groupId), mapped)
      groupHistoryStates.value = {
        ...groupHistoryStates.value,
        [groupId]: {
          hasMoreHistory: Boolean(history.hasMore),
          nextBeforeServerMessageId: String(history.nextBeforeServerMessageId || ''),
          historyLoaded: true,
        },
      }
    } catch (error) {
      pushEvent('error', (error as Error).message || '群历史消息加载失败')
    } finally {
      loadingHistory.value = false
    }
  }

  async function loadOlderMessages() {
    const stream = messageStream.value
    const previousHeight = stream?.scrollHeight || 0
    if (activeTargetType.value === 'group') {
      if (!activeGroupId.value) {
        return
      }
      await loadGroupHistoryPage(activeGroupId.value, true)
    } else {
      if (!activePeerUid.value) {
        return
      }
      await loadHistoryPage(activePeerUid.value, true)
    }
    await nextTick()
    if (stream) {
      const nextHeight = stream.scrollHeight
      stream.scrollTop = nextHeight - previousHeight
    }
  }

  async function markConversationRead(peerUid: string) {
    if (!peerUid || readingConversation.value) {
      return
    }

    upsertConversation(peerUid, { unreadCount: 0 })
    readingConversation.value = true
    try {
      await api.post<void>('/me/im/conversations/read', undefined, {
        params: { targetId: peerUid },
      })
    } catch (error) {
      pushEvent('error', (error as Error).message || '清空未读失败')
    } finally {
      readingConversation.value = false
    }
  }

  function connectSocket() {
    if (!currentToken.value) {
      return
    }
    if (socket.value && (socket.value.readyState === WebSocket.OPEN || socket.value.readyState === WebSocket.CONNECTING)) {
      return
    }

    const url = new URL(wsUrl.value)
    url.searchParams.set('token', currentToken.value)

    connectionState.value = 'connecting'
    pushEvent('ws', `开始连接 ${url.toString()}`)

    const ws = new WebSocket(url.toString())
    socket.value = ws

    ws.addEventListener('open', () => {
      connectionState.value = 'live'
      startHeartbeat()
      pushEvent('ws', 'WebSocket 已连接。')
    })

    ws.addEventListener('message', (event) => {
      handleSocketMessage(String(event.data || ''))
    })

    ws.addEventListener('close', () => {
      stopHeartbeat()
      socket.value = null
      connectionState.value = 'idle'
      pushEvent('ws', 'WebSocket 已关闭。')
    })

    ws.addEventListener('error', () => {
      connectionState.value = 'error'
      pushEvent('error', 'WebSocket 发生错误。')
    })
  }

  function disconnectSocket() {
    stopHeartbeat()
    if (socket.value) {
      socket.value.close()
      socket.value = null
    }
    connectionState.value = 'idle'
  }

  function startHeartbeat() {
    stopHeartbeat()
    heartbeatTimer.value = window.setInterval(() => {
      if (!socket.value || socket.value.readyState !== WebSocket.OPEN) {
        return
      }
      socket.value.send(JSON.stringify({ type: 'heartbeat' }))
    }, 30000)
  }

  function stopHeartbeat() {
    if (heartbeatTimer.value != null) {
      window.clearInterval(heartbeatTimer.value)
      heartbeatTimer.value = null
    }
  }

  function handleSocketMessage(raw: string) {
    let packet: WsPacket
    try {
      packet = JSON.parse(raw)
    } catch {
      pushEvent('raw', raw)
      return
    }

    const type = packet.type || 'unknown'
    pushEvent(type, JSON.stringify(packet.data || packet.message || packet, null, 2))

    if (type === 'heartbeat_ack') {
      return
    }
    if (type === 'send_message_accepted') {
      handleAccepted(packet.data as AcceptedPayload | undefined)
      return
    }
    if (type === 'message_received') {
      void handleReceived(packet.data as MessagePushPayload | undefined)
      return
    }
    if (type === 'conversation_updated') {
      void handleConversationUpdated(packet.data as ConversationUpdatedPayload | undefined)
      return
    }
    if (type === 'group_conversation_updated') {
      handleGroupConversationUpdated(packet.data as GroupConversationUpdatedPayload | undefined)
      return
    }
    if (type === 'error') {
      connectionState.value = 'error'
    }
  }

  function handleAccepted(data?: AcceptedPayload) {
    if (!data) {
      return
    }

    const clientKey = String(data.clientMessageId || '')
    const pending = pendingMessages.value[clientKey] || findPendingByConversationId(data.conversationId || '')
    if (!pending) {
      return
    }

    pending.pending = false
    pending.senderLocation = String(data.senderLocation || pending.senderLocation || '')
    pending.time = formatDateTime(data.sendTime)
    pending.epoch = toEpoch(data.sendTime)

    const nextPending = { ...pendingMessages.value }
    delete nextPending[pending.clientKey]
    pendingMessages.value = nextPending
  }

  async function handleReceived(data?: MessagePushPayload) {
    if (!data) {
      return
    }
    if (isGroupConversationId(data.conversationId)) {
      await handleReceivedGroupMessage(data)
      return
    }
    await handleReceivedSingleMessage(data)
  }

  async function handleReceivedSingleMessage(data: MessagePushPayload) {
    const senderId = String(data.senderId || '')
    const receiverId = String(data.receiverId || '')
    const clientKey = String(data.clientMessageId || '')
    const peerUid = senderId === currentUid.value ? receiverId : senderId

    await loadPeerProfile(peerUid)
    ensureConversation(peerUid)

    if (senderId === currentUid.value && clientKey) {
      clearPendingMessage(clientKey, data)
    }

    mergeMessages(buildStreamKey('single', peerUid), [toRealtimeSingleMessageItem(data, peerUid)])

    const preview = buildConversationPreview(data.content)
    upsertConversation(peerUid, {
      conversationId: data.conversationId || buildSingleConversationId(currentUid.value, peerUid),
      lastMessage: preview,
      lastMessageTime: formatDateTime(data.sendTime),
      lastMessageEpoch: toEpoch(data.sendTime),
      unreadCount:
        (activeTargetType.value === 'single' && activePeerUid.value === peerUid) || senderId === currentUid.value
          ? 0
          : 1,
    })

    if (!activePeerUid.value && activeTargetType.value === 'single') {
      openConversation(peerUid)
      return
    }

    if (activeTargetType.value === 'single' && activePeerUid.value === peerUid && senderId !== currentUid.value) {
      void markConversationRead(peerUid)
      void nextTick().then(scrollStreamToBottom)
    }
  }

  async function handleReceivedGroupMessage(data: MessagePushPayload) {
    const groupId = resolveGroupId(data)
    if (!groupId) {
      return
    }
    const senderId = String(data.senderId || '')
    await Promise.all([
      loadGroupProfile(groupId),
      senderId && senderId !== currentUid.value ? loadPeerProfile(senderId) : Promise.resolve(),
    ])

    mergeMessages(buildStreamKey('group', groupId), [toRealtimeGroupMessageItem(data)])
    patchGroupProfileFromMessage(groupId, data)
    patchGroupWindowFromMessage(groupId, data)

    if (activeTargetType.value === 'group' && activeGroupId.value === groupId) {
      void nextTick().then(scrollStreamToBottom)
    }
  }

  async function handleConversationUpdated(data?: ConversationUpdatedPayload) {
    if (!data?.conversationId) {
      return
    }
    const peerUid = applyConversationWindow(data)
    if (!peerUid) {
      return
    }
    await loadPeerProfile(peerUid)
  }

  function handleGroupConversationUpdated(data?: GroupConversationUpdatedPayload) {
    if (!data?.groupId) {
      return
    }
    const groupId = String(data.groupId)
    if (!groupWindows.value[groupId]) {
      return
    }
    upsertGroupWindow(groupId, {
      lastServerMessageId: data.lastServerMessageId,
    })
  }

  async function sendMessage() {
    const text = messageDraft.value.trim()
    const imageUrls = draftImages.value.map((item) => item.uploadedUrl).filter(Boolean)
    if (!text && !imageUrls.length) {
      return
    }
    if (hasUploadingImages.value) {
      pushEvent('ui', '图片还在上传，请稍候发送。')
      return
    }
    if (!socket.value || socket.value.readyState !== WebSocket.OPEN) {
      pushEvent('ui', '实时通道还没连上。')
      return
    }

    const clientMessageId = Date.now()
    const clientKey = String(clientMessageId)
    const peerUid = activePeerUid.value
    const groupId = activeGroupId.value
    const receiverId = activeTargetType.value === 'group' ? groupId : peerUid
    if (!receiverId) {
      return
    }
    const messageType = resolveMessageType(text, imageUrls)
    const streamKey = buildStreamKey(activeTargetType.value, receiverId)

    const optimistic: MessageItem = {
      id: '',
      serverMessageId: '',
      dedupeKey: `${currentUid.value}:${clientMessageId}`,
      direction: 'outgoing',
      senderId: currentUid.value,
      senderLocation: '',
      time: '发送中',
      epoch: Date.now(),
      text,
      imageUrls,
      pending: true,
      peerUid: activeTargetType.value === 'group' ? currentUid.value : peerUid,
      clientKey,
    }

    mergeMessages(streamKey, [optimistic])
    pendingMessages.value = {
      ...pendingMessages.value,
      [clientKey]: optimistic,
    }
    if (activeTargetType.value === 'group') {
      upsertGroupWindow(groupId, {
        conversationId: `g_${groupId}`,
        lastMessage: buildConversationPreview({ text, imageUrls }),
        lastMessageTime: new Date().toISOString(),
        unreadCount: 0,
      })
    } else {
      upsertConversation(peerUid, {
        conversationId: buildSingleConversationId(currentUid.value, peerUid),
        lastMessage: buildConversationPreview({ text, imageUrls }),
        lastMessageTime: '刚刚',
        lastMessageEpoch: Date.now(),
        unreadCount: 0,
      })
    }

    socket.value.send(
      JSON.stringify({
        type: 'send_message',
        conversationType: activeTargetType.value === 'group' ? 2 : 1,
        receiverId,
        clientMessageId,
        messageType,
        content: {
          text,
          imageUrls,
        },
      }),
    )

    messageDraft.value = ''
    revokeDraftPreviews()
    draftImages.value = []
    uploadError.value = ''
    await nextTick()
    scrollStreamToBottom()
  }

  function openConversation(peerUid: string) {
    if (!peerUid) {
      return
    }
    sidebarTab.value = 'single'
    router.replace({
      name: 'messages',
      query: { peerUid },
    })
  }

  function openGroupConversation(groupId: string) {
    if (!groupId) {
      return
    }
    sidebarTab.value = 'group'
    router.replace({
      name: 'messages',
      query: { groupId },
    })
  }

  function setSidebarTab(nextTab: ActiveTargetType) {
    if (sidebarTab.value === nextTab && activeTargetType.value === nextTab) {
      return
    }

    sidebarTab.value = nextTab

    if (nextTab === 'single') {
      const firstSingleConversation = sortedSingleConversations.value[0]
      if (firstSingleConversation) {
        openConversation(firstSingleConversation.peerUid)
        return
      }
      router.replace({ name: 'messages' })
      activeTargetType.value = 'single'
      activePeerUid.value = ''
      activeGroupId.value = ''
      return
    }

    const firstGroupConversation = sortedGroupConversations.value[0]
    if (firstGroupConversation?.groupId) {
      openGroupConversation(String(firstGroupConversation.groupId))
      return
    }
    router.replace({ name: 'messages' })
    activeTargetType.value = 'group'
    activePeerUid.value = ''
    activeGroupId.value = ''
  }

  function ensureConversation(peerUid: string) {
    if (!peerUid || conversations.value[peerUid]) {
      return
    }
    upsertConversation(peerUid, {
      conversationId: currentUid.value ? buildSingleConversationId(currentUid.value, peerUid) : '',
      lastMessage: '',
      lastMessageTime: '',
      lastMessageEpoch: 0,
      unreadCount: 0,
      hasMoreHistory: false,
      nextBeforeServerMessageId: '',
      historyLoaded: false,
    })
  }

  function applyGroupConversationWindow(data?: GroupConversationWindowVO) {
    const groupId = String(data?.groupId || '').trim()
    if (!groupId) {
      return ''
    }

    const shouldClearUnread =
      activeTargetType.value === 'group' && activeGroupId.value === groupId

    upsertGroupWindow(groupId, {
      conversationId: String(data?.conversationId || `g_${groupId}`),
      groupId,
      groupName: data?.groupName,
      groupAvatar: data?.groupAvatar,
      status: data?.status,
      memberCount: data?.memberCount,
      isAllMuted: data?.isAllMuted,
      lastMessage: String(data?.lastMessage || ''),
      lastMessageTime: String(data?.lastMessageTime || ''),
      lastServerMessageId: data?.lastServerMessageId,
      lastMessageSeq: data?.lastMessageSeq,
      lastReadSeq: data?.lastReadSeq,
      unreadCount: shouldClearUnread ? 0 : Number(data?.unreadCount || 0),
      isMuted: data?.isMuted,
    })
    return groupId
  }

  function ensureGroupHistoryState(groupId: string) {
    if (!groupId || groupHistoryStates.value[groupId]) {
      return
    }
    groupHistoryStates.value = {
      ...groupHistoryStates.value,
      [groupId]: {
        hasMoreHistory: false,
        nextBeforeServerMessageId: '',
        historyLoaded: false,
      },
    }
  }

  function applyConversationWindow(data?: ConversationUpdatedPayload | ConversationWindowVO) {
    if (!data?.conversationId) {
      return ''
    }

    const peerUid = String(
      data.targetId ||
      (data as ConversationUpdatedPayload).targetUserId ||
      resolvePeerUidFromConversation(data.conversationId, currentUid.value),
    )
    if (!peerUid) {
      return ''
    }

    ensureConversation(peerUid)
    upsertConversation(peerUid, {
      conversationId: data.conversationId,
      lastMessage: data.lastMessage || '',
      lastMessageTime: formatDateTime(data.lastMessageTime),
      lastMessageEpoch: toEpoch(data.lastMessageTime),
      unreadCount: Number(data.unreadCount || 0),
    })
    return peerUid
  }

  function upsertConversation(peerUid: string, patch: Partial<ConversationItem>) {
    const current = conversations.value[peerUid] || {
      peerUid,
      conversationId: '',
      lastMessage: '',
      lastMessageTime: '',
      lastMessageEpoch: 0,
      unreadCount: 0,
      hasMoreHistory: false,
      nextBeforeServerMessageId: '',
      historyLoaded: false,
    }

    conversations.value = {
      ...conversations.value,
      [peerUid]: {
        ...current,
        ...patch,
        peerUid,
      },
    }
  }

  function upsertGroupWindow(groupId: string, patch: Partial<GroupConversationWindowVO>) {
    const current = groupWindows.value[groupId] || {
      groupId,
      conversationId: `g_${groupId}`,
      lastMessage: '',
      lastMessageTime: '',
      unreadCount: 0,
    }

    groupWindows.value = {
      ...groupWindows.value,
      [groupId]: {
        ...current,
        ...patch,
        groupId,
      },
    }
  }

  function mergeMessages(streamKey: string, incoming: MessageItem[]) {
    const current = messagesByStream.value[streamKey] || []
    const merged = new Map<string, MessageItem>()

    for (const item of current) {
      merged.set(item.dedupeKey, item)
    }
    for (const item of incoming) {
      const existing = merged.get(item.dedupeKey)
      merged.set(item.dedupeKey, existing ? { ...existing, ...item } : item)
    }

    const next = Array.from(merged.values()).sort((left, right) => {
      if (left.epoch === right.epoch) {
        return left.dedupeKey.localeCompare(right.dedupeKey)
      }
      return left.epoch - right.epoch
    })

    messagesByStream.value = {
      ...messagesByStream.value,
      [streamKey]: next,
    }
  }

  async function uploadImages(files: File[]) {
    if (!files.length) {
      return
    }

    uploadError.value = ''

    for (const file of files) {
      const localId = `${Date.now()}-${Math.random().toString(16).slice(2)}`
      const previewUrl = URL.createObjectURL(file)
      draftImages.value = [
        ...draftImages.value,
        {
          localId,
          previewUrl,
          uploadedUrl: '',
          uploading: true,
          error: '',
        },
      ]

      try {
        const form = new FormData()
        form.append('file', file)
        const uploadedUrl = await api.post<string>('/me/im/uploads/images', form, {
          headers: {
            'Content-Type': 'multipart/form-data',
          },
        })
        draftImages.value = draftImages.value.map((item) =>
          item.localId === localId
            ? { ...item, uploadedUrl, uploading: false, error: '' }
            : item,
        )
      } catch (error) {
        const message = (error as Error).message || '图片上传失败'
        uploadError.value = message
        draftImages.value = draftImages.value.map((item) =>
          item.localId === localId
            ? { ...item, uploadedUrl: '', uploading: false, error: message }
            : item,
        )
      }
    }
  }

  function removeDraftImage(localId: string) {
    const target = draftImages.value.find((item) => item.localId === localId)
    if (target?.previewUrl) {
      URL.revokeObjectURL(target.previewUrl)
    }
    draftImages.value = draftImages.value.filter((item) => item.localId !== localId)
  }

  function revokeDraftPreviews() {
    for (const item of draftImages.value) {
      if (item.previewUrl) {
        URL.revokeObjectURL(item.previewUrl)
      }
    }
  }

  function resolvePeerName(peerUid: string) {
    if (peerUid === currentUid.value) {
      return currentProfile.value?.nickname || currentUsername.value || '我'
    }
    return peerProfiles.value[peerUid]?.nickname || `UID ${peerUid}`
  }

  function resolvePeerAvatar(peerUid: string) {
    if (peerUid === currentUid.value) {
      return currentProfile.value?.avatar || ''
    }
    return peerProfiles.value[peerUid]?.avatar || ''
  }

  function resolveMessagePeerName(item: MessageItem) {
    return resolvePeerName(item.peerUid)
  }

  function resolveGroupName(groupId: string) {
    return groupWindows.value[groupId]?.groupName || `群聊 ${groupId}`
  }

  function resolveGroupAvatar(groupId: string) {
    return groupWindows.value[groupId]?.groupAvatar || ''
  }

  function resolveGroupMemberRole(role?: number) {
    if (Number(role || 0) === 1) return '群主'
    if (Number(role || 0) === 2) return '管理员'
    return '成员'
  }

  function resolveGroupMemberStatus(status?: number) {
    if (Number(status || 0) === 1) return '正常'
    if (Number(status || 0) === 2) return '已离开'
    if (Number(status || 0) === 3) return '已移出'
    return '未知状态'
  }

  function pushEvent(type: string, body: string) {
    eventLogs.value = [
      {
        type,
        body,
        time: formatClock(new Date()),
      },
      ...eventLogs.value,
    ].slice(0, 40)
  }

  function buildStreamKey(type: ActiveTargetType, rawId: string) {
    return type === 'group' ? `g:${rawId}` : `s:${rawId}`
  }

  function isGroupConversationId(conversationId?: string) {
    return /^g_\d+$/.test(String(conversationId || ''))
  }

  function resolveGroupId(data: MessagePushPayload) {
    const receiverId = String(data.receiverId || '').trim()
    if (/^\d+$/.test(receiverId)) {
      return receiverId
    }
    const match = /^g_(\d+)$/.exec(String(data.conversationId || ''))
    return match?.[1] || ''
  }

  function patchGroupProfileFromMessage(groupId: string, data: MessagePushPayload) {
    if (!groupId) {
      return
    }
    const current = groupProfiles.value[groupId]
    if (!current) {
      return
    }
    groupProfiles.value = {
      ...groupProfiles.value,
      [groupId]: {
        ...current,
        lastMessage: buildConversationPreview(data.content),
        lastMessageTime: data.sendTime,
        lastServerMessageId: data.serverMessageId,
      },
    }
  }

  function patchGroupWindowFromMessage(groupId: string, data: MessagePushPayload) {
    const current = groupWindows.value[groupId]
    if (!current) {
      return
    }
    upsertGroupWindow(groupId, {
      lastMessage: buildConversationPreview(data.content),
      lastMessageTime: String(data.sendTime || ''),
      lastServerMessageId: data.serverMessageId,
      unreadCount:
        activeTargetType.value === 'group' && activeGroupId.value === groupId
          ? 0
          : Number(current.unreadCount || 0) + 1,
    })
  }

  function resolvePeerUidFromConversation(conversationId: string, uid: string) {
    const match = /^single_(\d+)_(\d+)$/.exec(String(conversationId || ''))
    if (!match) {
      return ''
    }
    return match[1] === uid ? match[2] : match[1]
  }

  function buildSingleConversationId(first: string, second: string) {
    if (!/^\d+$/.test(first) || !/^\d+$/.test(second)) {
      return first <= second ? `single_${first}_${second}` : `single_${second}_${first}`
    }

    const left = BigInt(first)
    const right = BigInt(second)
    return left <= right ? `single_${first}_${second}` : `single_${second}_${first}`
  }

  function findPendingByConversationId(conversationId: string) {
    return Object.values(pendingMessages.value).find((item) =>
      buildSingleConversationId(currentUid.value, item.peerUid) === conversationId,
    )
  }

  function clearPendingMessage(clientKey: string, data?: MessagePushPayload) {
    const pending = pendingMessages.value[clientKey]
    if (!pending) {
      return
    }

    pending.pending = false
    pending.serverMessageId = String(data?.serverMessageId || pending.serverMessageId || '')
    pending.senderLocation = String(data?.senderLocation || pending.senderLocation || '')
    pending.time = formatDateTime(data?.sendTime) || pending.time
    pending.epoch = toEpoch(data?.sendTime) || pending.epoch

    const nextPending = { ...pendingMessages.value }
    delete nextPending[clientKey]
    pendingMessages.value = nextPending
  }

  function buildConversationPreview(content?: MessageContent) {
    const text = String(content?.text || '').trim()
    const imageCount = content?.imageUrls?.filter(Boolean).length || 0
    if (text && imageCount > 0) {
      return `${text} [图片]`
    }
    if (text) {
      return text
    }
    if (imageCount > 1) {
      return `[图片 ${imageCount}]`
    }
    if (imageCount === 1) {
      return '[图片]'
    }
    return '[空消息]'
  }

  function resolveMessageType(text: string, imageUrls: string[]) {
    if (text && imageUrls.length) {
      return MESSAGE_TYPE_RICH
    }
    if (imageUrls.length) {
      return MESSAGE_TYPE_IMAGE
    }
    return MESSAGE_TYPE_TEXT
  }

  function buildRealtimeDedupeKey(senderId: string, serverMessageId: string, clientKey: string) {
    if (serverMessageId) {
      return `server:${serverMessageId}`
    }
    return `${senderId}:${clientKey}`
  }

  function toRealtimeSingleMessageItem(data: MessagePushPayload, peerUid: string): MessageItem {
    const senderId = String(data.senderId || '')
    const serverMessageId = String(data.serverMessageId || '')
    const clientKey = String(data.clientMessageId || Date.now())
    return {
      id: '',
      serverMessageId,
      dedupeKey: buildRealtimeDedupeKey(senderId, serverMessageId, clientKey),
      direction: senderId === currentUid.value ? 'outgoing' : 'incoming',
      senderId,
      senderLocation: String(data.senderLocation || ''),
      time: formatDateTime(data.sendTime),
      epoch: toEpoch(data.sendTime),
      text: String(data.content?.text || ''),
      imageUrls: (data.content?.imageUrls || []).filter(Boolean),
      pending: false,
      peerUid,
      clientKey,
    }
  }

  function toRealtimeGroupMessageItem(data: MessagePushPayload): MessageItem {
    const senderId = String(data.senderId || '')
    const serverMessageId = String(data.serverMessageId || '')
    const clientKey = String(data.clientMessageId || Date.now())
    return {
      id: '',
      serverMessageId,
      dedupeKey: buildRealtimeDedupeKey(senderId, serverMessageId, clientKey),
      direction: senderId === currentUid.value ? 'outgoing' : 'incoming',
      senderId,
      senderLocation: String(data.senderLocation || ''),
      time: formatDateTime(data.sendTime),
      epoch: toEpoch(data.sendTime),
      text: String(data.content?.text || ''),
      imageUrls: (data.content?.imageUrls || []).filter(Boolean),
      pending: false,
      peerUid: senderId,
      clientKey,
    }
  }

  function toSingleMessageItem(record: MessageVO, peerUid: string): MessageItem {
    const senderId = String(record.senderId || '')
    const serverMessageId = String(record.serverMessageId || '')
    const clientKey = String(record.clientMessageId || record.id || Date.now())
    return {
      id: String(record.id || ''),
      serverMessageId,
      dedupeKey: buildRealtimeDedupeKey(senderId, serverMessageId, clientKey),
      direction: senderId === currentUid.value ? 'outgoing' : 'incoming',
      senderId,
      senderLocation: String(record.senderLocation || ''),
      time: formatDateTime(record.sendTime),
      epoch: toEpoch(record.sendTime),
      text: String(record.content?.text || ''),
      imageUrls: (record.content?.imageUrls || []).filter(Boolean),
      pending: false,
      peerUid,
      clientKey,
    }
  }

  function toGroupMessageItem(record: MessageVO): MessageItem {
    const senderId = String(record.senderId || '')
    const serverMessageId = String(record.serverMessageId || '')
    const clientKey = String(record.clientMessageId || record.id || Date.now())
    return {
      id: String(record.id || ''),
      serverMessageId,
      dedupeKey: buildRealtimeDedupeKey(senderId, serverMessageId, clientKey),
      direction: senderId === currentUid.value ? 'outgoing' : 'incoming',
      senderId,
      senderLocation: String(record.senderLocation || ''),
      time: formatDateTime(record.sendTime),
      epoch: toEpoch(record.sendTime),
      text: String(record.content?.text || ''),
      imageUrls: (record.content?.imageUrls || []).filter(Boolean),
      pending: false,
      peerUid: senderId,
      clientKey,
    }
  }

  function scrollStreamToBottom() {
    if (!messageStream.value) {
      return
    }
    messageStream.value.scrollTop = messageStream.value.scrollHeight
  }

  function formatDateTime(value?: string) {
    if (!value) return ''
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return String(value)
    return formatClock(date)
  }

  function formatClock(date: Date) {
    return new Intl.DateTimeFormat('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    }).format(date)
  }

  function toEpoch(value?: string) {
    if (!value) return 0
    const date = new Date(value)
    return Number.isNaN(date.getTime()) ? 0 : date.getTime()
  }

  return {
    activeConversation,
    activeConversationSubtitle,
    activeConversationTitle,
    activeGroupId,
    activeGroupMembers,
    activeGroupProfile,
    activeGroupRole,
    activeHasMoreHistory,
    activeMessages,
    activePeerProfile,
    activePeerUid,
    activeTargetType,
    actionTargetUserId,
    canSend,
    canManageActiveGroup,
    closeGroupSettings,
    connectionLabel,
    connectionState,
    connectSocket,
    currentProfile,
    currentUid,
    currentUsername,
    disconnectSocket,
    draftImages,
    eventLogs,
    groupAvatarUploading,
    groupMembersError,
    groupMembersLoading,
    groupMuteUpdating,
    groupNameDraft,
    groupNameSaving,
    groupSettingsTab,
    groupSettingsVisible,
    hasFailedImages,
    hasUploadingImages,
    inviteGroupMember,
    inviteGroupMemberError,
    inviteGroupMemberLoading,
    inviteGroupMemberUid,
    kickGroupMember,
    loadOlderMessages,
    loadingHistory,
    loadGroupMembers,
    messageDraft,
    messageStream,
    moderationError,
    openConversation,
    openGroupConversation,
    openGroupSettings,
    profileError,
    removeDraftImage,
    resolveGroupAvatar,
    resolveGroupMemberRole,
    resolveGroupMemberStatus,
    resolveGroupName,
    resolveMessagePeerName,
    resolvePeerAvatar,
    resolvePeerName,
    saveGroupName,
    sendMessage,
    setGroupNameDraft,
    setGroupSettingsTab,
    setInviteGroupMemberUid,
    setSidebarTab,
    sidebarTab,
    setMessageDraft: (value: string) => {
      messageDraft.value = value
    },
    sortedGroupConversations,
    sortedSingleConversations,
    toggleGroupMuteStatus,
    toggleMemberMute,
    updateMemberRole,
    uploadError,
    uploadGroupAvatar,
    uploadImages,
  }
}
