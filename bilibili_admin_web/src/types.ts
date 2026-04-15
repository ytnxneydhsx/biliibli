export type UserLoginVO = {
  uid: string | null
  username: string
  roleCode: number
  token: string
}

export const ADMIN_ROLE_CODE = 1

export type CursorPageVO<T> = {
  records: T[]
  nextCursor: string | null
  hasMore: boolean
}

export type PageVO<T> = {
  records: T[]
  total: number
  pageNo: number
  pageSize: number
  totalPages: number
}

export type AdminVideoVO = {
  id: string
  authorUid: string
  title: string
  description: string
  coverUrl: string
  videoUrl: string
  duration: number
  createTime: string
  nickname: string
}

export type AdminUserVO = {
  uid: string
  username: string
  roleCode: number
  status: number
  nickname: string
  avatar: string
  sign: string
  likeEnabled: boolean
  commentEnabled: boolean
  imMessageSendEnabled: boolean
  videoUploadEnabled: boolean
  profileEditEnabled: boolean
  videoBusinessBanned: boolean
}
