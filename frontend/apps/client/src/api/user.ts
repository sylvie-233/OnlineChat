import http from './index'
import type { UserInfo, UserSetting } from '@/types'

export const userApi = {
  getById(id: number) {
    return http.get<{ code: number; data: UserInfo }>(`/api/user/${id}`)
  },
  search(keyword: string) {
    return http.get<{ code: number; data: UserInfo[] }>('/api/user/search', { params: { keyword } })
  },
  updateProfile(data: Partial<UserInfo>) {
    return http.put('/api/user/profile', data)
  },
  updateOnlineStatus(status: number) {
    return http.put('/api/user/online-status', { status })
  },
  getSettings() {
    return http.get<{ code: number; data: UserSetting }>('/api/user/settings')
  },
  updateSettings(data: Partial<UserSetting>) {
    return http.put('/api/user/settings', data)
  },
  getSessions() {
    return http.get('/api/user/sessions')
  },
  kickSession(sessionId: number) {
    return http.post(`/api/user/sessions/${sessionId}/kick`)
  },
}
