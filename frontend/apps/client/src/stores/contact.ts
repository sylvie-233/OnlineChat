import { defineStore } from 'pinia'
import { contactApi } from '@/api/contact'
import { userApi } from '@/api/user'
import type { Contact, ContactGroup, FriendRequest, BlockInfo, UserInfo } from '@/types'

export const useContactStore = defineStore('contact', () => {
  const contacts = ref<Contact[]>([])
  const groups = ref<ContactGroup[]>([])
  const requests = ref<FriendRequest[]>([])
  const blocklist = ref<BlockInfo[]>([])
  // reactive Map 保证 .set() 触发UI更新
  const userCache = reactive<Map<number, UserInfo>>(new Map())
  const loading = ref(false)

  async function loadAll() {
    loading.value = true
    try {
      const [c, g, r, b] = await Promise.all([
        contactApi.getList(),
        contactApi.getGroups(),
        contactApi.getRequests(),
        contactApi.getBlocklist(),
      ])
      if (c.data.code === 200) contacts.value = c.data.data || []
      if (g.data.code === 200) groups.value = g.data.data || []
      if (r.data.code === 200) requests.value = r.data.data || []
      if (b.data.code === 200) blocklist.value = b.data.data || []
    } finally {
      loading.value = false
    }
  }

  async function getUserInfo(userId: number): Promise<UserInfo | null> {
    if (userCache.has(userId)) return userCache.get(userId)!
    try {
      const { data } = await userApi.getById(userId)
      if (data.code === 200) {
        userCache.set(userId, data.data)
        return data.data
      }
    } catch (e) { /* ignore */ }
    return null
  }

  async function sendRequest(toUserId: number, verifyMessage = '', source = 'search') {
    await contactApi.sendRequest(toUserId, verifyMessage, source)
    await loadAll()
  }

  async function handleRequest(requestId: number, agree: boolean) {
    await contactApi.handleRequest(requestId, agree)
    await loadAll()
  }

  async function deleteContact(contactUserId: number) {
    await contactApi.deleteContact(contactUserId)
    await loadAll()
  }

  async function blockUser(blockedUserId: number, reason?: string) {
    await contactApi.blockUser(blockedUserId, reason)
    await loadAll()
  }

  async function unblockUser(blockedUserId: number) {
    await contactApi.unblockUser(blockedUserId)
    await loadAll()
  }

  async function updateRemark(contactUserId: number, remark: string) {
    await contactApi.updateRemark(contactUserId, remark)
    await loadAll()
  }

  return {
    contacts, groups, requests, blocklist, userCache, loading,
    loadAll, getUserInfo, sendRequest, handleRequest,
    deleteContact, blockUser, unblockUser, updateRemark,
  }
})
