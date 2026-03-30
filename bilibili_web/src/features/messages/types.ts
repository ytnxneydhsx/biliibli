import type { UserProfileVO } from '../../types'

export type ConnectionState = 'idle' | 'connecting' | 'live' | 'error'

export type ConversationItem = {
  peerUid: string
  conversationId: string
  lastMessage: string
  lastMessageTime: string
  lastMessageEpoch: number
  unreadCount: number
  hasMoreHistory: boolean
  nextBeforeMessageId: string
  historyLoaded: boolean
}

export type MessageContent = {
  text?: string
  imageUrls?: string[]
}

export type MessageItem = {
  id: string
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
  nextBeforeMessageId?: string | number | null
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
