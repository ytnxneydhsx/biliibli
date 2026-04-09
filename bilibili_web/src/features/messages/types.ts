import type { UserProfileVO } from '../../types'

export type ConnectionState = 'idle' | 'connecting' | 'live' | 'error'
export type ActiveTargetType = 'single' | 'group'

export type ConversationItem = {
  peerUid: string
  conversationId: string
  lastMessage: string
  lastMessageTime: string
  lastMessageEpoch: number
  unreadCount: number
  hasMoreHistory: boolean
  nextBeforeServerMessageId: string
  historyLoaded: boolean
}

export type MessageContent = {
  text?: string
  imageUrls?: string[]
}

export type MessageItem = {
  id: string
  serverMessageId: string
  dedupeKey: string
  direction: 'incoming' | 'outgoing'
  senderId: string
  senderLocation: string
  time: string
  epoch: number
  text: string
  imageUrls: string[]
  pending: boolean
  peerUid: string
  clientKey: string
}

export type MessagePushPayload = {
  conversationId?: string
  senderId?: string | number
  receiverId?: string | number
  serverMessageId?: string | number
  clientMessageId?: string | number
  senderLocation?: string
  messageType?: number
  sendTime?: string
  content?: MessageContent
}

export type ConversationUpdatedPayload = {
  conversationId?: string
  targetId?: string | number
  targetUserId?: string | number
  lastMessage?: string
  lastMessageTime?: string
  unreadCount?: number
  isMuted?: number
}

export type ConversationWindowVO = {
  conversationId?: string
  targetId?: string | number
  lastMessage?: string
  lastMessageTime?: string
  unreadCount?: number
  isMuted?: number
}

export type ConversationWindowListVO = {
  ownerUserId?: string | number
  size?: number
  records?: ConversationWindowVO[]
}

export type GroupConversationWindowVO = {
  conversationId?: string
  groupId?: string | number
  groupName?: string
  groupAvatar?: string
  status?: number
  memberCount?: number
  isAllMuted?: number
  lastMessage?: string
  lastMessageTime?: string
  lastServerMessageId?: string | number
  lastMessageSeq?: string | number
  lastReadSeq?: string | number
  unreadCount?: number
  isMuted?: number
}

export type GroupConversationWindowListVO = {
  ownerUserId?: string | number
  size?: number
  records?: GroupConversationWindowVO[]
}

export type GroupProfileVO = {
  groupId: string | number
  conversationId?: string
  groupName?: string
  ownerUserId?: string | number
  groupAvatar?: string
  status?: number
  memberCount?: number
  isAllMuted?: number
  lastMessage?: string
  lastMessageTime?: string
  lastServerMessageId?: string | number
  lastMessageSeq?: string | number
}

export type GroupConversationUpdatedPayload = {
  groupId?: string | number
  lastServerMessageId?: string | number
}

export type AcceptedPayload = {
  conversationId?: string
  clientMessageId?: string | number
  senderLocation?: string
  sendTime?: string
}

export type WsPacket<T = unknown> = {
  type?: string
  code?: number
  message?: string
  data?: T
}

export type MessageVO = {
  id: string | number
  serverMessageId?: string | number
  conversationId?: string
  senderId?: string | number
  receiverId?: string | number
  clientMessageId?: string | number
  senderLocation?: string
  messageType?: number
  content?: MessageContent
  sendTime?: string
  status?: number
}

export type MessageHistoryVO = {
  records: MessageVO[]
  hasMore: boolean
  nextBeforeServerMessageId?: string | number | null
}

export type DraftImageItem = {
  localId: string
  previewUrl: string
  uploadedUrl: string
  uploading: boolean
  error: string
}

export type EventLogItem = {
  type: string
  body: string
  time: string
}

export type PeerProfilesMap = Record<string, UserProfileVO>
