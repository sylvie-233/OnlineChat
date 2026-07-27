import http from './index'
import type { Contact, ContactGroup, FriendRequest, BlockInfo } from '@/types'

export const contactApi = {
  getGroups() {
    return http.get<{ code: number; data: ContactGroup[] }>('/api/contact/groups')
  },
  createGroup(groupName: string) {
    return http.post('/api/contact/groups', { groupName })
  },
  renameGroup(groupId: number, groupName: string) {
    return http.put(`/api/contact/groups/${groupId}`, { groupName })
  },
  deleteGroup(groupId: number) {
    return http.delete(`/api/contact/groups/${groupId}`)
  },
  getList() {
    return http.get<{ code: number; data: Contact[] }>('/api/contact/list')
  },
  deleteContact(contactUserId: number) {
    return http.delete(`/api/contact/${contactUserId}`)
  },
  updateRemark(contactUserId: number, remark: string) {
    return http.put('/api/contact/remark', { contactUserId, remark })
  },
  toggleStar(contactUserId: number, starred: boolean) {
    return http.put('/api/contact/star', { contactUserId, starred })
  },
  moveToGroup(contactUserId: number, groupId: number) {
    return http.put('/api/contact/move-group', { contactUserId, groupId })
  },
  sendRequest(toUserId: number, verifyMessage: string, source: string) {
    return http.post('/api/contact/request', { toUserId, verifyMessage, source })
  },
  getRequests() {
    return http.get<{ code: number; data: FriendRequest[] }>('/api/contact/requests')
  },
  handleRequest(requestId: number, agree: boolean) {
    return http.put(`/api/contact/request/${requestId}`, { agree })
  },
  blockUser(blockedUserId: number, reason?: string) {
    return http.post(`/api/contact/block/${blockedUserId}`, { reason })
  },
  unblockUser(blockedUserId: number) {
    return http.delete(`/api/contact/block/${blockedUserId}`)
  },
  getBlocklist() {
    return http.get<{ code: number; data: BlockInfo[] }>('/api/contact/blocks')
  },
}
