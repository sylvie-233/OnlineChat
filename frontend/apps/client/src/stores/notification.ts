import { defineStore } from 'pinia'
import { notifApi } from '@/api/notification'
import type { Notification } from '@/types'

export const useNotifStore = defineStore('notification', () => {
  const list = ref<Notification[]>([])
  const unreadCount = ref(0)
  const loading = ref(false)
  const total = ref(0)
  const currentPage = ref(1)
  const pageSize = ref(20)
  const keyword = ref('')
  const isRead = ref<number | undefined>(undefined)
  // 暂存搜索条件，点击搜索时才应用
  const pendingKeyword = ref('')
  const pendingIsRead = ref<number | undefined>(undefined)

  async function loadList(page = 1) {
    loading.value = true
    try {
      const { data } = await notifApi.getList(page, pageSize.value, keyword.value, isRead.value)
      if (data.code === 200) {
        list.value = data.data || []
        total.value = (data as any).total || 0
        currentPage.value = page
      }
    } finally { loading.value = false }
  }

  function doSearch() {
    keyword.value = pendingKeyword.value
    isRead.value = pendingIsRead.value
    loadList(1)
  }

  function doReset() {
    pendingKeyword.value = ''
    pendingIsRead.value = undefined
    keyword.value = ''
    isRead.value = undefined
    pageSize.value = 20
    loadList(1)
  }

  function setPageSize(size: number) {
    pageSize.value = size
    loadList(1)
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
    const item = list.value.find(n => n.id === id)
    if (item) item.isRead = 1
  }

  async function markAllRead() {
    await notifApi.markAllRead()
    unreadCount.value = 0
    list.value = list.value.map(n => ({ ...n, isRead: 1 }))
  }

  return { list, unreadCount, loading, total, currentPage, pageSize, keyword, isRead,
    pendingKeyword, pendingIsRead, loadList, doSearch, doReset, setPageSize, loadUnreadCount, markRead, markAllRead }
})
