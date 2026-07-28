import { defineStore } from 'pinia'
import { chatApi } from '@/api/chat'
import { wsClient } from '@/api/ws'
import { CMD, type Conversation, type Message } from '@/types'
import { useAuthStore } from './auth'
import { useContactStore } from './contact'

export const useChatStore = defineStore('chat', () => {
  const conversations = ref<Conversation[]>([])
  const activeId = ref<number>(0)
  const activeType = ref<number>(0)
  const activeTargetId = ref<number>(0) // 对方用户ID/群ID（发送消息时用）
  const activeName = ref('')
  const messages = ref<Message[]>([])
  const loading = ref(false)

  const auth = useAuthStore()

  const activeConv = computed(() =>
    conversations.value.find((c: Conversation) => c.id === activeId.value),
  )

  // ==================== 会话 ====================
  async function loadConversations() {
    try {
      const { data } = await chatApi.getConversations()
      if (data.code === 200) {
        conversations.value = (data.data || []).map((c: Conversation) => {
          // 尝试从联系人 store 获取名称
          if (!c.targetName || c.targetName.startsWith('用户')) {
            const contactStore = useContactStore()
            const user = contactStore.userCache.get(c.targetId)
            if (user) c.targetName = user.nickname || user.username
          }
          return c
        })
      }
    } catch (e) { /* handled by interceptor */ }
  }

  let loadReqId = 0

  function openConversation(conv: Conversation) {
    if (!conv.id || conv.id === 0) {
      const exist = conversations.value.find(
        (c: Conversation) => c.type === conv.type && c.targetId === conv.targetId
      )
      if (exist) conv = exist
    }
    activeId.value = conv.id
    activeType.value = conv.type
    activeTargetId.value = conv.targetId
    activeName.value = conv.targetName || `用户${conv.targetId}`
    messages.value = []
    loadMessages()
  }

  // ==================== 消息 ====================
  async function loadMessages() {
    // 允许 activeId=0（私聊按 userId 查），跳过快照加载
    if (!activeId.value && !activeTargetId.value) { loading.value = false; return }
    loading.value = true
    const reqId = ++loadReqId
    try {
      const { data } = await chatApi.getLatest(activeId.value, activeType.value, 30)
      if (reqId !== loadReqId) return // 竞态: 忽略过期响应
      if (data.code === 200) {
        messages.value = (data.data || []).reverse()
      }
    } finally {
      if (reqId === loadReqId) loading.value = false
    }
  }

  async function loadMore() {
    if (messages.value.length === 0) return
    const cursorTime = messages.value[0].sendTime || ''
    try {
      const { data } = await chatApi.getHistory(activeId.value, activeType.value, cursorTime, 30)
      if (data.code === 200 && data.data) {
        messages.value = [...data.data.reverse(), ...messages.value]
      }
    } catch (e) { /* ignore */ }
  }

  function addMessage(msg: Message) {
    messages.value.push(msg)
  }

  // ==================== WebSocket ====================
  let wsInited = false

  function connectWs() {
    if (wsInited) return
    if (wsClient.connected) return
    wsInited = true

    const token = auth.token
    const userId = auth.userId

    // 收到第一个 HEARTBEAT_ACK 即发送认证（一次性）
    const doAuth = () => {
      wsClient.send(CMD.AUTH, { token, userId, deviceType: 'web', deviceId: 'web-' + userId })
      wsClient.off(CMD.HEARTBEAT_ACK) // 只发一次
    }
    wsClient.on(CMD.HEARTBEAT_ACK, doAuth)

    wsClient.on(CMD.AUTH_ACK, () => {
      console.log('[IM] 认证成功')
      loadConversations()
    })

    wsClient.on(CMD.PUSH_MSG, (p) => {
      const msg = p.body as Message
      const targetId = n(msg.conversationType === 0 ? msg.fromUserId : msg.toId)
      let idx = conversations.value.findIndex(
        (c: Conversation) => c.type === n(msg.conversationType) && c.targetId === targetId
      )
      // 会话不存在则创建临时会话（id=0，openConversation 会重新匹配）
      if (idx < 0) {
        conversations.value.unshift({
          id: 0,
          userId: auth.userId,
          type: n(msg.conversationType),
          targetId,
          lastMessageId: Number(msg.id) || 0,
          lastMessageSeq: msg.seq,
          unreadCount: 1,
          isPinned: 0,
          isMuted: 0,
          isHidden: 0,
          draft: '',
          targetName: `用户${targetId}`,
          lastContent: getPreview(msg),
          updateTime: msg.createTime || msg.sendTime,
        } as Conversation)
      } else {
        conversations.value[idx].lastContent = getPreview(msg)
        conversations.value[idx].lastMessageSeq = msg.seq
        if (msg.conversationId !== activeId.value || !activeId.value) {
          conversations.value[idx].unreadCount++
        }
      }
      // 当前会话则追加到消息列表
      if ((n(msg.conversationId) === activeId.value || activeTargetId.value === targetId)
          && n(msg.conversationType) === activeType.value) {
        messages.value.push(msg)
        wsClient.send(CMD.READ_NOTIFY, { messageId: msg.id })
      }
    })

    // ACK：替换乐观消息为真实消息 + 更新 activeId
    wsClient.on(CMD.PRIVATE_MSG_ACK, (p) => {
      const msg = p.body as Message
      if (msg) {
        if (!activeId.value && (msg.conversationId || msg.id)) {
          activeId.value = Number(msg.conversationId) || Number(msg.id)
        }
        // 替换乐观消息（按 content+fromUserId 匹配）
        const tempIdx = messages.value.findIndex(m =>
          (!m.id || m.status === 0) && n(m.fromUserId) === auth.userId && m.content === msg.content
        )
        if (tempIdx >= 0) messages.value[tempIdx] = msg
        else if (!messages.value.find(m => m.id === msg.id)) messages.value.push(msg)
        loadConversations()
      }
    })
    wsClient.on(CMD.GROUP_MSG_ACK, (p) => {
      const msg = p.body as Message
      if (msg) {
        if (!activeId.value && (msg.conversationId || msg.id)) {
          activeId.value = Number(msg.conversationId) || Number(msg.id)
        }
        const tempIdx = messages.value.findIndex(m =>
          (!m.id || m.status === 0) && n(m.fromUserId) === auth.userId && m.content === msg.content
        )
        if (tempIdx >= 0) messages.value[tempIdx] = msg
        else if (!messages.value.find(m => m.id === msg.id)) messages.value.push(msg)
        loadConversations()
      }
    })

    wsClient.connect(`ws://${location.host}/ws`)
  }

  function sendWsMessage(content: string, msgType = 0, toId: number) {
    const body: any = {
      msgType,
      content,
      clientMsgId: `${auth.userId}_${Date.now()}`,
    }
    if (activeType.value === 1) {
      body.groupId = toId
    } else {
      body.toUserId = toId
    }

    // 乐观更新：立即显示"发送中"
    const tempMsg: Message = {
      id: 0,
      seq: 0,
      conversationType: activeType.value,
      conversationId: activeId.value || 0,
      fromUserId: auth.userId,
      toId: toId,
      msgType,
      content,
      extra: '',
      replyToMsgId: 0,
      status: 0, // SENDING
      sendTime: new Date().toISOString(),
      createTime: new Date().toISOString(),
      isRecalled: 0,
    }
    messages.value.push(tempMsg)

    if (wsClient.connected) {
      const cmd = activeType.value === 1 ? CMD.GROUP_MSG : CMD.PRIVATE_MSG
      wsClient.send(cmd, body)
    } else {
      console.warn('[IM] WS未连接，使用HTTP降级发送')
      chatApi.sendMessage({
        ...body,
        toId,
        conversationType: activeType.value,
        conversationId: activeId.value,
      }).then((res: any) => {
        const msg = res?.data?.data
        if (msg) {
          if (!activeId.value) activeId.value = Number(msg.conversationId) || Number(msg.id)
          messages.value.push(msg)
        }
      }).catch(() => {})
    }
  }

  // ==================== 会话操作 ====================
  async function setPinned(id: number, pinned: boolean) {
    await chatApi.setPinned(id, pinned)
    await loadConversations()
  }

  async function setMuted(id: number, muted: boolean) {
    await chatApi.setMuted(id, muted)
  }

  async function deleteConv(id: number) {
    await chatApi.deleteConversation(id)
    if (activeId.value === id) {
      activeId.value = 0
      messages.value = []
    }
    await loadConversations()
  }

  // 统一转 number，兜底防御 WS/HTTP 序列化差异
  function n(v: any): number { return v == null ? 0 : Number(v) }

  function getPreview(msg: Message): string {
    if (!msg) return ''
    if (n(msg.msgType) === 1) return '[图片]'
    if (n(msg.msgType) === 2) return '[语音]'
    if (n(msg.msgType) === 3) return '[视频]'
    if (n(msg.msgType) === 4) return '[文件]'
    if (n(msg.msgType) === 5) return '[位置]'
    return (msg.content || '').substring(0, 50)
  }

  function reset() {
    wsInited = false
    wsClient.close()
    conversations.value = []
    messages.value = []
    activeId.value = 0
  }

  return {
    conversations, activeId, activeType, activeTargetId, activeName, messages, loading, activeConv,
    loadConversations, openConversation, loadMessages, loadMore, addMessage,
    connectWs, sendWsMessage, setPinned, setMuted, deleteConv, reset,
  }
})
