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
  const loading = ref(false)

  async function loadMyGroups() {
    loading.value = true
    try {
      // 从会话列表中筛选所有 type=1 的群聊
      const useChatStore = (await import('@/stores/chat')).useChatStore
      const chat = useChatStore()
      const groupIds = chat.conversations
        .filter((c: any) => c.type === 1)
        .map((c: any) => c.targetId)
      // 加载每个群的信息
      const infos: GroupInfo[] = []
      for (const id of groupIds) {
        try {
          const { data } = await groupApi.getById(id)
          if (data.code === 200 && data.data) infos.push(data.data)
        } catch (e) { /* skip */ }
      }
      myGroups.value = infos
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

  async function publishAnnouncement(groupId: number, title: string, content: string) {
    await groupApi.publishAnnouncement(groupId, title, content)
    await openGroup(groupId)
  }

  async function loadInvitations() {
    try {
      const { data } = await groupApi.getInvitations()
      if (data.code === 200) invitations.value = data.data || []
    } catch (e) { /* ignore */ }
  }

  return {
    myGroups, currentGroup, members, announcements, requests, invitations, loading,
    loadMyGroups, openGroup, createGroup, updateSettings, kickMember,
    setMemberRole, publishAnnouncement, loadInvitations,
  }
})
