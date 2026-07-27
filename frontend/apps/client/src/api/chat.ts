import http from './index'
import type { Conversation, Message, MessageReaction, MessageBookmark } from '@/types'

export const chatApi = {
  getConversations() {
    return http.get<{ code: number; data: Conversation[] }>('/api/conversation/list')
  },
  setPinned(id: number, pinned: boolean) {
    return http.put(`/api/conversation/${id}/pin`, { pinned })
  },
  setMuted(id: number, muted: boolean) {
    return http.put(`/api/conversation/${id}/mute`, { muted })
  },
  clearUnread(id: number) {
    return http.put(`/api/conversation/${id}/clear-unread`)
  },
  saveDraft(id: number, draft: string) {
    return http.put(`/api/conversation/${id}/draft`, { draft })
  },
  deleteConversation(id: number) {
    return http.delete(`/api/conversation/${id}`)
  },
  sendMessage(data: any) {
    return http.post('/api/message/send', data)
  },
  getLatest(conversationId: number, type: number, limit = 20) {
    return http.get<{ code: number; data: Message[] }>('/api/message/latest', {
      params: { conversationId, type, limit },
    })
  },
  getHistory(conversationId: number, type: number, cursorSeq: number, limit = 20) {
    return http.get<{ code: number; data: Message[] }>('/api/message/history', {
      params: { conversationId, type, cursorSeq, limit },
    })
  },
  syncMessages(conversationId: number, type: number, sinceSeq: number, limit = 50) {
    return http.get<{ code: number; data: Message[] }>('/api/message/sync', {
      params: { conversationId, type, sinceSeq, limit },
    })
  },
  recallMessage(messageId: number, reason?: string) {
    return http.put(`/api/message/${messageId}/recall`, { reason })
  },
  retryMessage(messageId: number) {
    return http.post(`/api/message/${messageId}/retry`)
  },
  markRead(messageId: number) {
    return http.put(`/api/message/${messageId}/read`)
  },
  getReadCount(messageId: number) {
    return http.get<{ code: number; data: number }>(`/api/message/${messageId}/read-count`)
  },
  addReaction(messageId: number, emoji: string) {
    return http.post(`/api/message/${messageId}/reaction`, { emoji })
  },
  removeReaction(messageId: number, emoji: string) {
    return http.delete(`/api/message/${messageId}/reaction`, { data: { emoji } })
  },
  getReactions(messageId: number) {
    return http.get<{ code: number; data: Record<string, MessageReaction[]> }>(
      `/api/message/${messageId}/reactions`,
    )
  },
  bookmark(messageId: number, tag: string) {
    return http.post(`/api/message/${messageId}/bookmark`, { tag })
  },
  removeBookmark(bookmarkId: number) {
    return http.delete(`/api/message/bookmark/${bookmarkId}`)
  },
  getBookmarks(page = 1, size = 20, tag?: string) {
    return http.get<{ code: number; data: MessageBookmark[] }>('/api/message/bookmarks', {
      params: { page, size, tag },
    })
  },
  getMentions(page = 1, size = 20) {
    return http.get('/api/message/mentions', { params: { page, size } })
  },
  getUnreadMentionCount() {
    return http.get<{ code: number; data: number }>('/api/message/mentions/unread-count')
  },
}
