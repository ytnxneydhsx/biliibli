import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../../../lib/api'
import { authState } from '../../../lib/auth'
import type { UserProfileVO } from '../../../types'
import type {
  AcceptedPayload,
  ConnectionState,
  ConversationItem,
  ConversationWindowListVO,
  ConversationWindowVO,
  ConversationUpdatedPayload,
  DraftImageItem,
  EventLogItem,
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

export function useMessagesPage() {
  const route = useRoute()
  const router = useRouter()

  const socket = ref<WebSocket | null>(null)
  const heartbeatTimer = ref<number | null>(null)
  const connectionState = ref<ConnectionState>('idle')
  const eventLogs = ref<EventLogItem[]>([])
  const conversations = ref<Record<string, ConversationItem>>({})
  const messagesByPeer = ref<Record<string, MessageItem[]>>({})
  const pendingMessages = ref<Record<string, MessageItem>>({})
  const peerProfiles = ref<Record<string, UserProfileVO>>({})
  const loadingPeer = ref(false)
  const loadingConversations = ref(false)
  const loadingHistory = ref(false)
  const readingConversation = ref(false)
  const uploadError = ref('')
  const messageDraft = ref('')
  const draftImages = ref<DraftImageItem[]>([])
  const messageStream = ref<HTMLDivElement | null>(null)
  const activePeerUid = ref('')

  const currentUid = computed(() => authState.uid || '')
  const currentToken = computed(() => authState.token || '')
  const currentProfile = computed(() => authState.profile)
  const currentUsername = computed(() => authState.username || '')
  const peerUidFromRoute = computed(() => {
    const value = String(route.query.peerUid ?? '').trim()
    return /^\d+$/.test(value) ? value : ''
  })

  const connectionLabel = computed(() => {
    if (connectionState.value === 'live') return '已连接'
    if (connectionState.value === 'connecting') return '连接中'
    if (connectionState.value === 'error') return '连接异常'
    return '未连接'
  })

  const sortedConversations = computed(() =>
    Object.values(conversations.value).sort((left, right) => right.lastMessageEpoch - left.lastMessageEpoch),
  )

  const activeMessages = computed(() => {
    if (!activePeerUid.value) return []
    return messagesByPeer.value[activePeerUid.value] || []
  })

  const activeConversation = computed(() => {
    if (!activePeerUid.value) return null
    return conversations.value[activePeerUid.value] || null
  })

  const activePeerProfile = computed(() => {
    if (!activePeerUid.value) return null
    return peerProfiles.value[activePeerUid.value] || null
  })

  const activeConversationTitle = computed(() => {
    if (!activePeerUid.value) return '还没有打开任何会话'
    if (activePeerProfile.value?.nickname) return `与 ${activePeerProfile.value.nickname} 的对话`
    return `与 UID ${activePeerUid.value} 的对话`
  })

  const activeConversationSubtitle = computed(() => {
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

  const hasUploadingImages = computed(() => draftImages.value.some((item) => item.uploading))
  const hasFailedImages = computed(() => draftImages.value.some((item) => !!item.error))
  const canSend = computed(() => {
    if (!activePeerUid.value) return false
    if (hasUploadingImages.value) return false
    return !!messageDraft.value.trim() || draftImages.value.some((item) => !!item.uploadedUrl)
  })

  const wsUrl = computed(() => {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    return `${protocol}//${window.location.host}/ws/im`
  })

  watch(
    peerUidFromRoute,
    async (peerUid) => {
      if (!peerUid) {
        activePeerUid.value = ''
        return
      }
      await activateConversation(peerUid)
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
    if (peerUidFromRoute.value) {
      await loadPeerProfile(peerUidFromRoute.value)
    }
  })

  onBeforeUnmount(() => {
    disconnectSocket()
    revokeDraftPreviews()
  })

  async function activateConversation(peerUid: string) {
    ensureConversation(peerUid)
    activePeerUid.value = peerUid
    await Promise.all([
      loadPeerProfile(peerUid),
      ensureHistoryLoaded(peerUid),
    ])
    await markConversationRead(peerUid)
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

  async function ensureHistoryLoaded(peerUid: string) {
    const conversation = conversations.value[peerUid]
    if (conversation?.historyLoaded) {
      return
    }
    await loadHistoryPage(peerUid, false)
  }

  async function loadConversationWindows() {
    if (!currentToken.value || loadingConversations.value) {
      return
    }

    loadingConversations.value = true
    try {
      const payload = await api.get<ConversationWindowListVO>('/me/im/conversations')
      const records = payload.records || []
      const loadedPeerUids: string[] = []
      for (const record of records) {
        const peerUid = applyConversationWindow(record)
        if (peerUid) {
          loadedPeerUids.push(peerUid)
        }
      }
      await Promise.all(
        loadedPeerUids
          .map((peerUid) => loadPeerProfile(peerUid)),
      )
      if (!peerUidFromRoute.value && !activePeerUid.value && loadedPeerUids.length > 0) {
        openConversation(loadedPeerUids[0])
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
      const mapped = (history.records || []).map((record) => toMessageItem(record, peerUid))
      mergeMessages(peerUid, mapped)
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

  async function loadOlderMessages() {
    if (!activePeerUid.value) {
      return
    }
    const stream = messageStream.value
    const previousHeight = stream?.scrollHeight || 0
    await loadHistoryPage(activePeerUid.value, true)
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

    const senderId = String(data.senderId || '')
    const receiverId = String(data.receiverId || '')
    const clientKey = String(data.clientMessageId || '')
    const peerUid = senderId === currentUid.value ? receiverId : senderId

    await loadPeerProfile(peerUid)
    ensureConversation(peerUid)

    if (senderId === currentUid.value && clientKey) {
      clearPendingMessage(clientKey, data)
    }

    mergeMessages(peerUid, [toRealtimeMessageItem(data, peerUid)])

    const preview = buildConversationPreview(data.content)
    upsertConversation(peerUid, {
      conversationId: data.conversationId || buildSingleConversationId(currentUid.value, peerUid),
      lastMessage: preview,
      lastMessageTime: formatDateTime(data.sendTime),
      lastMessageEpoch: toEpoch(data.sendTime),
      unreadCount: activePeerUid.value === peerUid || senderId === currentUid.value ? 0 : 1,
    })

    if (!activePeerUid.value) {
      openConversation(peerUid)
      return
    }

    if (activePeerUid.value === peerUid && senderId !== currentUid.value) {
      void markConversationRead(peerUid)
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

  async function sendMessage() {
    const text = messageDraft.value.trim()
    const imageUrls = draftImages.value.map((item) => item.uploadedUrl).filter(Boolean)
    if ((!text && !imageUrls.length) || !activePeerUid.value) {
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
    const messageType = resolveMessageType(text, imageUrls)

    const optimistic: MessageItem = {
      id: '',
      dedupeKey: `${currentUid.value}:${clientMessageId}`,
      direction: 'outgoing',
      senderId: currentUid.value,
      senderLocation: '',
      time: '发送中',
      epoch: Date.now(),
      text,
      imageUrls,
      pending: true,
      peerUid,
      clientKey,
    }

    mergeMessages(peerUid, [optimistic])
    pendingMessages.value = {
      ...pendingMessages.value,
      [clientKey]: optimistic,
    }
    upsertConversation(peerUid, {
      conversationId: buildSingleConversationId(currentUid.value, peerUid),
      lastMessage: buildConversationPreview({ text, imageUrls }),
      lastMessageTime: '刚刚',
      lastMessageEpoch: Date.now(),
      unreadCount: 0,
    })

    socket.value.send(
      JSON.stringify({
        type: 'send_message',
        receiverId: peerUid,
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
    router.replace({
      name: 'messages',
      query: { peerUid },
    })
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

  function applyConversationWindow(data?: ConversationUpdatedPayload | ConversationWindowVO) {
    if (!data?.conversationId) {
      return ''
    }

    const peerUid = String(data.targetId || (data as ConversationUpdatedPayload).targetUserId || resolvePeerUidFromConversation(data.conversationId, currentUid.value))
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

  function mergeMessages(peerUid: string, incoming: MessageItem[]) {
    const current = messagesByPeer.value[peerUid] || []
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

    messagesByPeer.value = {
      ...messagesByPeer.value,
      [peerUid]: next,
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
    return peerProfiles.value[peerUid]?.nickname || `UID ${peerUid}`
  }

  function resolvePeerAvatar(peerUid: string) {
    return peerProfiles.value[peerUid]?.avatar || ''
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
    return Object.values(pendingMessages.value).find((item) => buildSingleConversationId(currentUid.value, item.peerUid) === conversationId)
  }

  function clearPendingMessage(clientKey: string, data?: MessagePushPayload) {
    const pending = pendingMessages.value[clientKey]
    if (!pending) {
      return
    }

    pending.pending = false
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

  function toRealtimeMessageItem(data: MessagePushPayload, peerUid: string): MessageItem {
    const senderId = String(data.senderId || '')
    const clientKey = String(data.clientMessageId || Date.now())
    return {
      id: '',
      dedupeKey: `${senderId}:${clientKey}`,
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

  function toMessageItem(record: MessageVO, peerUid: string): MessageItem {
    const senderId = String(record.senderId || '')
    const clientKey = String(record.clientMessageId || record.id || Date.now())
    return {
      id: String(record.id || ''),
      dedupeKey: `${senderId}:${clientKey}`,
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
    activeMessages,
    activePeerProfile,
    activePeerUid,
    canSend,
    connectionLabel,
    connectionState,
    connectSocket,
    currentProfile,
    currentUid,
    currentUsername,
    disconnectSocket,
    draftImages,
    eventLogs,
    hasFailedImages,
    hasUploadingImages,
    loadOlderMessages,
    loadingHistory,
    messageDraft,
    messagesByPeer,
    messageStream,
    openConversation,
    removeDraftImage,
    resolvePeerAvatar,
    resolvePeerName,
    sendMessage,
    setMessageDraft: (value: string) => {
      messageDraft.value = value
    },
    sortedConversations,
    uploadError,
    uploadImages,
  }
}
