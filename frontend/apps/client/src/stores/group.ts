import { defineStore } from 'pinia'
import { groupApi } from '@/api/group'
import type { GroupInfo, GroupMember, GroupAnnouncement, GroupRequest } from '@/types'

export const useGroupStore = defineStore('group', () => {
  const myGroups = ref<GroupInfo[]>([])
  const currentGroup = ref<GroupInfo | null>(null)
  const members = ref<GroupMember[]>([])
  const announcements = ref<GroupAnnouncement[]>([])
  const requests = ref<GroupRequest[]>([])
  const invitations = ref<GroupRequest[]>([])
  const groupNameCache = reactive<Map<number, string>>(new Map())
  const loading = ref(false)

  async function loadMyGroups() {
    loading.value = true
    try {
      const { data } = await groupApi.getMyGroups()
      if (data.code === 200) myGroups.value = data.data || []
    } finally {
      loading.value = false
    }
  }

  async function openGroup(groupId: number) {
    loading.value = true
    try {
      const [info, m, a] = await Promise.all([
        groupApi.getById(groupId),
        groupApi.getMembers(groupId),
        groupApi.getAnnouncements(groupId),
      ])
      if (info.data.code === 200) currentGroup.value = info.data.data
      if (m.data.code === 200) members.value = m.data.data || []
      if (a.data.code === 200) announcements.value = a.data.data || []
      // 加载入群申请（后台静默，不阻塞）
      try {
        const r = await groupApi.getRequests(groupId)
        if (r.data.code === 200) requests.value = r.data.data || []
      } catch { /* ignore */ }
      // 预加载成员用户信息
      const useContactStore = (await import('@/stores/contact')).useContactStore
      for (const member of members.value) {
        useContactStore().getUserInfo(member.userId)
      }
    } finally {
      loading.value = false
    }
  }

  async function createGroup(groupName: string) {
    await groupApi.create(groupName)
    await loadMyGroups()
  }

  async function updateSettings(groupId: number, data: any) {
    await groupApi.updateSettings(groupId, data)
    if (currentGroup.value?.id === groupId) await openGroup(groupId)
  }

  async function kickMember(groupId: number, userId: number) {
    await groupApi.kickMember(groupId, userId)
    await openGroup(groupId)
  }

  async function setMemberRole(groupId: number, userId: number, role: number) {
    await groupApi.setMemberRole(groupId, userId, role)
    await openGroup(groupId)
  }

  async function setMemberNickname(groupId: number, userId: number, nickname: string) {
    await groupApi.setMemberNickname(groupId, userId, nickname)
    await openGroup(groupId)
  }

  async function leaveGroup(groupId: number) {
    await groupApi.leave(groupId)
    currentGroup.value = null
    await loadMyGroups()
  }

  async function publishAnnouncement(groupId: number, title: string, content: string) {
    await groupApi.publishAnnouncement(groupId, title, content)
    await openGroup(groupId)
  }

  async function deleteAnnouncement(annId: number, groupId: number) {
    await groupApi.deleteAnnouncement(annId)
    await openGroup(groupId)
  }

  async function markAnnouncementRead(annId: number) {
    await groupApi.markAnnouncementRead(annId)
  }

  async function toggleAnnouncementPin(annId: number, pinned: boolean, groupId: number) {
    await groupApi.toggleAnnouncementPin(annId, pinned)
    await openGroup(groupId)
  }

  async function loadRequests(groupId: number) {
    try {
      const { data } = await groupApi.getRequests(groupId)
      if (data.code === 200) requests.value = data.data || []
    } catch (e) { /* ignore */ }
  }

  async function handleRequest(requestId: number, agree: boolean, groupId: number) {
    await groupApi.handleRequest(requestId, agree)
    await openGroup(groupId)
  }

  async function loadInvitations() {
    try {
      const { data } = await groupApi.getInvitations()
      if (data.code === 200) invitations.value = data.data || []
      // 预加载群名称
      for (const r of invitations.value) {
        if (!groupNameCache.has(r.groupId)) {
          try {
            const { data: g } = await groupApi.getById(r.groupId)
            if (g.code === 200 && g.data) groupNameCache.set(r.groupId, g.data.groupName)
          } catch { /* skip */ }
        }
      }
    } catch (e) { /* ignore */ }
  }

  async function acceptInvitation(requestId: number, groupId: number) {
    await groupApi.handleRequest(requestId, true)
    await loadInvitations()
    await loadMyGroups()
  }

  return {
    myGroups, currentGroup, members, announcements, requests, invitations, groupNameCache, loading,
    loadMyGroups, openGroup, createGroup, updateSettings, kickMember,
    setMemberRole, setMemberNickname, leaveGroup, publishAnnouncement,
    deleteAnnouncement, markAnnouncementRead, toggleAnnouncementPin,
    loadRequests, handleRequest, acceptInvitation, loadInvitations,
  }
})
