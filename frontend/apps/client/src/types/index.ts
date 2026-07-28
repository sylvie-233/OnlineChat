// ==================== 通用 ====================
export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar: string
  phone: string
  email: string
  gender: number
  bio: string
  birthday: string
  region: string
  status: number
  onlineStatus: number
  lastLoginTime: string
}

export interface UserSetting {
  id: number
  userId: number
  msgNotifyEnabled: number
  soundEnabled: number
  vibrateEnabled: number
  showDetailEnabled: number
  friendVerifyType: number
  groupInviteVerify: number
  theme: string
  language: string
  fontSize: string
  chatBgUrl: string
}

// ==================== 联系人 ====================
export interface ContactGroup {
  id: number
  userId: number
  groupName: string
  sortOrder: number
}

export interface Contact {
  id: number
  userId: number
  contactUserId: number
  groupId: number
  remark: string
  isStarred: number
  source: string
}

export interface FriendRequest {
  id: number
  fromUserId: number
  toUserId: number
  verifyMessage: string
  status: number // 0=待处理 1=同意 2=拒绝
  handledTime: string
  createTime: string
}

export interface BlockInfo {
  id: number
  userId: number
  blockedUserId: number
  reason: string
  createTime: string
}

// ==================== 会话 ====================
export interface Conversation {
  id: number
  userId: number
  type: number // 0=单聊 1=群聊
  targetId: number
  lastMessageId: number
  lastMessageSeq: number
  unreadCount: number
  isPinned: number
  isMuted: number
  isHidden: number
  draft: string
  updateTime: string
  // 前端扩展
  targetName?: string
  targetAvatar?: string
  lastContent?: string
}

// ==================== 消息 ====================
export interface Message {
  id: number  // Snowflake 46-bit < 2^53，安全落在 JS number 范围
  seq: number
  conversationType: number
  conversationId: number
  fromUserId: number
  toId: number
  msgType: number
  content: string
  extra: string
  replyToMsgId: number
  status: number
  sendTime: string
  createTime: string
  isRecalled: number
  // 前端扩展
  fromNickname?: string
  fromAvatar?: string
}

export interface MessageReaction {
  id: number
  messageId: number
  userId: number
  emoji: string
}

export interface MessageBookmark {
  id: number
  userId: number
  messageId: number
  tag: string
  createTime: string
}

// ==================== 群组 ====================
export interface GroupInfo {
  id: number
  groupName: string
  avatar: string
  ownerId: number
  announcement: string
  description: string
  maxMembers: number
  memberCount: number
  joinType: number
  isMutedAll: number
  status: number
}

export interface GroupMember {
  id: number
  groupId: number
  userId: number
  role: number // 0=成员 1=管理员 2=群主
  nicknameInGroup: string
  unreadCount: number
  isMuted: number
  isPinned: number
  joinTime: string
}

export interface GroupAnnouncement {
  id: number
  groupId: number
  publisherId: number
  title: string
  content: string
  isPinned: number
  createTime: string
}

export interface GroupRequest {
  id: number
  groupId: number
  fromUserId: number
  toUserId: number
  type: number
  verifyMessage: string
  status: number
  createTime: string
}

// ==================== 通知 ====================
export interface Notification {
  id: number
  userId: number
  type: number
  title: string
  content: string
  relatedId: number
  isRead: number
  readTime: string
  createTime: string
}

// ==================== 文件 ====================
export interface FileUpload {
  id: number
  userId: number
  fileName: string
  fileUrl: string
  fileType: string
  mimeType: string
  fileSize: number
  width: number
  height: number
  thumbnailUrl: string
  storageType: string
  status: number
}

// ==================== WebSocket ====================
export const CMD = {
  HEARTBEAT: 0,
  HEARTBEAT_ACK: 1,
  AUTH: 10,
  AUTH_ACK: 11,
  PRIVATE_MSG: 100,
  PRIVATE_MSG_ACK: 101,
  GROUP_MSG: 200,
  GROUP_MSG_ACK: 201,
  READ_NOTIFY: 300,
  RECALL_NOTIFY: 301,
  ONLINE_NOTIFY: 400,
  PUSH_MSG: 500,
  PUSH_NOTIFY: 501,
  TYPING: 600,
  TYPING_ACK: 601,
  FORWARD_MSG: 700,
  ERROR: -1,
} as const
