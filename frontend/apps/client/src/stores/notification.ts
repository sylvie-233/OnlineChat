import { defineStore } from 'pinia'
import { notifApi } from '@/api/notification'
import type { Notification } from '@/types'

export const useNotifStore = defineStore('notification', () => {
  const list = ref<Notification[]>([])
  const unreadCount = ref(0)
  const loading = ref(false)

  async function loadList(page = 1, size = 20) {
    loading.value = true
    try {
      const { data } = await notifApi.getList(page, size)
      if (data.code === 200) list.value = data.data || []
    } finally { loading.value = false }
  }

  async function loadUnreadCount() {
    try {
      const { data } = await notifApi.getUnreadCount()
      if (data.code === 200) unreadCount.value = data.data
    } catch (e) { /* ignore */ }
  }

  async function markRead(id: number) {
    await notifApi.markRead(id)
    unreadCount.value = Math.max(0, unreadCount.value - 1)
    await loadList()
  }

  async function markAllRead() {
    await notifApi.markAllRead()
    unreadCount.value = 0
    await loadList()
  }

  return { list, unreadCount, loading, loadList, loadUnreadCount, markRead, markAllRead }
})
