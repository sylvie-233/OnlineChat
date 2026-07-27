import http from './index'
import type { GroupInfo, GroupMember, GroupAnnouncement, GroupRequest } from '@/types'

export const groupApi = {
  create(groupName: string) {
    return http.post('/api/group', { groupName })
  },
  getById(groupId: number) {
    return http.get<{ code: number; data: GroupInfo }>(`/api/group/${groupId}`)
  },
  updateSettings(groupId: number, data: any) {
    return http.put(`/api/group/${groupId}/settings`, data)
  },
  dismiss(groupId: number) {
    return http.delete(`/api/group/${groupId}`)
  },
  getMembers(groupId: number) {
    return http.get<{ code: number; data: GroupMember[] }>(`/api/group/${groupId}/members`)
  },
  join(groupId: number) {
    return http.post(`/api/group/${groupId}/join`)
  },
  invite(groupId: number, inviteeId: number) {
    return http.post(`/api/group/${groupId}/invite/${inviteeId}`)
  },
  kickMember(groupId: number, userId: number) {
    return http.delete(`/api/group/${groupId}/member/${userId}`)
  },
  setMemberRole(groupId: number, userId: number, role: number) {
    return http.put(`/api/group/${groupId}/member/${userId}/role`, { role })
  },
  setMemberNickname(groupId: number, userId: number, nickname: string) {
    return http.put(`/api/group/${groupId}/member/${userId}/nickname`, { nickname })
  },
  applyJoin(groupId: number, verifyMessage: string) {
    return http.post(`/api/group/${groupId}/apply`, { verifyMessage })
  },
  getRequests(groupId: number) {
    return http.get<{ code: number; data: GroupRequest[] }>(`/api/group/${groupId}/requests`)
  },
  handleRequest(requestId: number, agree: boolean) {
    return http.put(`/api/group/request/${requestId}`, { agree })
  },
  getInvitations() {
    return http.get<{ code: number; data: GroupRequest[] }>('/api/group/invitations')
  },
  publishAnnouncement(groupId: number, title: string, content: string) {
    return http.post(`/api/group/${groupId}/announcement`, { title, content })
  },
  updateAnnouncement(announcementId: number, title: string, content: string) {
    return http.put(`/api/group/announcement/${announcementId}`, { title, content })
  },
  deleteAnnouncement(announcementId: number) {
    return http.delete(`/api/group/announcement/${announcementId}`)
  },
  getAnnouncements(groupId: number, page = 1, size = 20) {
    return http.get<{ code: number; data: GroupAnnouncement[] }>(
      `/api/group/${groupId}/announcements`,
      { params: { page, size } },
    )
  },
  markAnnouncementRead(announcementId: number) {
    return http.put(`/api/group/announcement/${announcementId}/read`)
  },
  getMessageReadStats(messageId: number) {
    return http.get(`/api/group/message/${messageId}/read-stats`)
  },
}
