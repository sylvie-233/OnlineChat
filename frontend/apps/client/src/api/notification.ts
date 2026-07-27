import http from './index'
import type { Notification } from '@/types'

export const notifApi = {
  getList(page = 1, size = 20) {
    return http.get<{ code: number; data: Notification[]; total: number }>(
      '/api/notification/list',
      { params: { page, size } },
    )
  },
  getUnreadCount() {
    return http.get<{ code: number; data: number }>('/api/notification/unread-count')
  },
  markRead(id: number) {
    return http.put(`/api/notification/${id}/read`)
  },
  markAllRead() {
    return http.put('/api/notification/read-all')
  },
}
