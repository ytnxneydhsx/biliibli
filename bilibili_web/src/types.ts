export type PageVO<T> = {
  records: T[]
  total: number
  pageNo: number
  pageSize: number
  totalPages: number
}

export type UserLoginVO = {
  uid: string
  username: string
  token: string
}

export type UserProfileVO = {
  uid: string
  nickname: string
  avatar: string
  sign: string
  followerCount: number
  followingCount: number
}

export type VideoVO = {
  id: string
  authorUid: string
  title: string
  coverUrl: string
  viewCount: number
  duration: number
  createTime: string
  nickname: string
}

export type VideoRankVO = {
  rank: number
  score: number
  id: string
  authorUid: string
  title: string
  coverUrl: string
  viewCount: number
  duration: number
  createTime: string
  nickname: string
}

export type VideoDetailVO = {
  id: string
  videoUrl: string
  title: string
  desc: string
  coverUrl: string
  duration: number
  uploadDate: string
  author: {
    uid: string
    nickname: string
    avatar: string
    sign: string
  }
  tags: string[]
  viewCount: number
  likeCount: number
  danmakuCount: number
  commentCount: number
  isLiked: boolean
  isFollowed: boolean
}

export type CommentVO = {
  id: string
  videoId: string
  uid: string
  parentId: string
  rootId: string
  content: string
  likeCount: number
  replyCount: number
  createTime: string
  nickname: string
  avatar: string
  isLiked: boolean
  childComments: CommentVO[]
}

export type FollowersQueryVO = {
  uid: string
  nickname: string
  avatar: string
  sign: string
}

export type UserSearchVO = {
  uid: string
  nickname: string
  avatar: string
  sign: string
  createTime: string
}

export type VideoUploadInitVO = {
  uploadId: string
  chunkSize: number
  totalChunks: number
  objectKey: string
  expireTime: string
}

export type VideoUploadSignedPartVO = {
  partNumber: number
  uploadUrl: string
}

export type VideoUploadPartSignVO = {
  uploadId: string
  expireTime: string
  parts: VideoUploadSignedPartVO[]
}

export type VideoUploadCompleteVO = {
  uploadId: string
  videoId: string
  videoUrl: string
}
