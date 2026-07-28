<script setup lang="ts">
import { useChatStore } from '@/stores/chat'
import { useContactStore } from '@/stores/contact'
import { chatApi } from '@/api/chat'
import { wsClient } from '@/api/ws'
import { useAuthStore } from '@/stores/auth'
import http from '@/api'
import { CMD, type Message } from '@/types'
import {
  SmileOutlined, PictureOutlined, FileOutlined,
  MoreOutlined, PushpinOutlined, CloseOutlined,
} from '@ant-design/icons-vue'
import MessageBubble from './MessageBubble.vue'

const chat = useChatStore()
const contact = useContactStore()
const auth = useAuthStore()
const inputText = ref('')
const msgList = ref<HTMLDivElement>()
const showConvMenu = ref(false)
const showEmoji = ref(false)
const uploadLoading = ref(false)
const isTyping = ref(false)
let typingTimer = 0

// 回复消息
const replyTo = ref<Message | null>(null)

// @提及
const showMention = ref(false)
const mentionFilter = ref('')
const mentionTargets = ref<any[]>([])

const activeMessages = computed(() => chat.messages)
const quickEmojis = ['😊','😂','❤️','👍','😢','🎉','🔥','👏','🙏','💪','🤔','😎','🌸','⭐','💯']

const needScroll = ref(false)

function scrollToBottom(force = false) {
  nextTick(() => {
    if (!msgList.value) return
    const el = msgList.value
    const atBottom = el.scrollTop + el.clientHeight >= el.scrollHeight - 80
    if (force || needScroll.value || atBottom) {
      el.scrollTop = el.scrollHeight
      needScroll.value = false
    }
  })
}

watch(() => chat.activeId, () => { needScroll.value = true; scrollToBottom(true) })
watch(() => chat.messages.length, () => scrollToBottom())
// 标记加载完成后需要滚到底部
watch(() => chat.loading, (val) => {
  if (!val) needScroll.value = true
  nextTick(() => scrollToBottom())
})

// 发送消息
function sendMessage() {
  const text = inputText.value.trim()
  if (!text && !replyTo.value) return
  if (!chat.activeTargetId) return

  const toId = chat.activeTargetId
  chat.sendWsMessage(text || '', 0, toId)
  inputText.value = ''
  replyTo.value = null
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

// @提及检测
function onInput() {
  if (!isTyping.value) {
    isTyping.value = true
    wsClient.send(CMD.TYPING, chat.activeType === 1
      ? { groupId: chat.activeTargetId || 0 }
      : { toUserId: chat.activeTargetId || 0 })
  }
  clearTimeout(typingTimer)
  typingTimer = window.setTimeout(() => { isTyping.value = false }, 2000)

  // 检测 @ 输入
  const cursor = inputText.value.lastIndexOf('@')
  if (cursor >= 0) {
    const after = inputText.value.substring(cursor + 1)
    if (!after.includes(' ') && after.length <= 10) {
      mentionFilter.value = after
      showMention.value = true
      loadMentionTargets()
      return
    }
  }
  showMention.value = false
}

async function loadMentionTargets() {
  // 群聊：加载群成员；单聊：只有对方
  if (chat.activeType === 1) {
    try {
      const { data } = await http.get(`/api/group/${chat.activeConv?.targetId}/members`)
      mentionTargets.value = (data.data || []).filter((m: any) => m.userId !== auth.userId)
    } catch (e) { mentionTargets.value = [] }
  }
}

function insertMention(user: any) {
  const cursor = inputText.value.lastIndexOf('@')
  const before = inputText.value.substring(0, cursor)
  const name = user.nicknameInGroup || user.nickname || `用户${user.userId}`
  inputText.value = before + `@{${user.userId}} `
  showMention.value = false
}

function setReplyTo(msg: Message) {
  replyTo.value = msg
}

function insertEmoji(emoji: string) {
  inputText.value += emoji
  showEmoji.value = false
}

// 图片上传
async function uploadImage() {
  const file = await selectFile('image/*')
  if (!file) return
  uploadLoading.value = true
  try {
    const form = new FormData(); form.append('file', file); form.append('fileType', 'image')
    const { data } = await http.post('/api/file/upload', form)
    if (data.code === 200) {
      const body: any = { msgType: 1, content: data.data.fileUrl, clientMsgId: `${auth.userId}_${Date.now()}` }
      const toId = chat.activeTargetId || 0
      if (chat.activeType === 1) body.groupId = toId; else body.toUserId = toId
      wsClient.send(chat.activeType === 1 ? CMD.GROUP_MSG : CMD.PRIVATE_MSG, body)
    }
  } catch (e) { /* ignore */ }
  finally { uploadLoading.value = false }
}

// 文件上传
async function uploadFile() {
  const file = await selectFile('*/*')
  if (!file) return
  uploadLoading.value = true
  try {
    const form = new FormData(); form.append('file', file); form.append('fileType', 'file')
    const { data } = await http.post('/api/file/upload', form)
    if (data.code === 200) {
      const body: any = { msgType: 4, content: data.data.fileUrl,
        extra: JSON.stringify({ fileName: file.name, fileSize: file.size }),
        clientMsgId: `${auth.userId}_${Date.now()}` }
      const toId = chat.activeTargetId || 0
      if (chat.activeType === 1) body.groupId = toId; else body.toUserId = toId
      wsClient.send(chat.activeType === 1 ? CMD.GROUP_MSG : CMD.PRIVATE_MSG, body)
    }
  } catch (e) { /* ignore */ }
  finally { uploadLoading.value = false }
}

function selectFile(accept: string): Promise<File | null> {
  return new Promise((resolve) => {
    const input = document.createElement('input')
    input.type = 'file'; input.accept = accept
    input.onchange = (e: any) => resolve(e.target?.files?.[0] || null)
    input.click()
  })
}

function handleScroll() {
  if (msgList.value && msgList.value.scrollTop === 0) chat.loadMore()
}

async function recallMessage(msg: Message) {
  await chatApi.recallMessage(msg.id)
}

function togglePin() {
  if (chat.activeId) chat.setPinned(chat.activeId, !chat.activeConv?.isPinned)
}
function toggleMute() {
  if (chat.activeId) chat.setMuted(chat.activeId, !chat.activeConv?.isMuted)
}

wsClient.on(CMD.TYPING_ACK, () => {
  isTyping.value = true
  clearTimeout(typingTimer)
  typingTimer = window.setTimeout(() => { isTyping.value = false }, 2000)
})
</script>

<template>
  <div class="chat-window">
    <!-- 标题栏 -->
    <div class="chat-titlebar">
      <div class="title-left">
        <span class="title-name">{{ chat.activeName }}</span>
        <span v-if="isTyping" class="typing-hint">对方正在输入...</span>
      </div>
      <div class="title-right">
        <a-button type="text" size="small" @click="togglePin"><PushpinOutlined /></a-button>
        <a-dropdown v-model:open="showConvMenu">
          <a-button type="text" size="small"><MoreOutlined /></a-button>
          <template #overlay>
            <a-menu>
              <a-menu-item @click="togglePin">{{ chat.activeConv?.isPinned ? '取消置顶' : '置顶' }}</a-menu-item>
              <a-menu-item @click="toggleMute">{{ chat.activeConv?.isMuted ? '取消免打扰' : '免打扰' }}</a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </div>
    </div>

    <!-- 消息区 -->
    <div class="msg-area" ref="msgList" @scroll="handleScroll">
      <a-spin v-if="chat.loading" size="small" class="msg-loading" />
      <div v-for="msg in activeMessages" :key="msg.id">
        <div v-if="msg.isRecalled" class="msg-notice">
          {{ Number(msg.fromUserId) === auth.userId ? '你' : '对方' }}撤回了一条消息
        </div>
        <MessageBubble
          v-else
          :message="msg"
          :is-mine="Number(msg.fromUserId) === auth.userId"
          @recall="recallMessage(msg)"
          @reply="setReplyTo(msg)"
        />
      </div>
    </div>

    <!-- 回复预览条 -->
    <div v-if="replyTo" class="reply-bar">
      <span class="reply-label">回复: {{ (replyTo.content || '').substring(0, 40) }}</span>
      <a-button type="text" size="small" @click="replyTo = null"><CloseOutlined /></a-button>
    </div>

    <!-- @提及下拉 -->
    <div v-if="showMention && mentionTargets.length" class="mention-dropdown">
      <div v-for="u in mentionTargets" :key="u.userId" class="mention-item" @click="insertMention(u)">
        {{ u.nicknameInGroup || `用户${u.userId}` }}
      </div>
    </div>

    <!-- 输入区 -->
    <div class="input-area">
      <div class="input-toolbar">
        <a-popover v-model:open="showEmoji" trigger="click" placement="top">
          <a-button type="text" size="small"><SmileOutlined /></a-button>
          <template #content>
            <div class="emoji-grid">
              <span v-for="e in quickEmojis" :key="e" class="emoji-item" @click="insertEmoji(e)">{{ e }}</span>
            </div>
          </template>
        </a-popover>
        <a-button type="text" size="small" @click="uploadImage"><PictureOutlined /></a-button>
        <a-button type="text" size="small" @click="uploadFile"><FileOutlined /></a-button>
      </div>
      <a-textarea
        v-model:value="inputText"
        :rows="3"
        placeholder="输入消息，Enter 发送，Shift+Enter 换行，输入 @ 提及成员"
        @keydown="handleKeydown"
        @input="onInput"
      />
      <div class="input-footer">
        <a-button type="primary" size="small" :loading="uploadLoading" @click="sendMessage">
          发送
        </a-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-window { display:flex; flex-direction:column; height:100%; }
.chat-titlebar { display:flex; justify-content:space-between; align-items:center;
  padding:10px 20px; background:#f5f5f5; border-bottom:1px solid #e0e0e0; height:56px; flex-shrink:0; }
.title-left { display:flex; align-items:center; gap:8px; }
.title-name { font-size:16px; font-weight:600; }
.typing-hint { font-size:12px; color:#07c160; }
.title-right { display:flex; gap:4px; }
.msg-area { flex:1; overflow-y:auto; padding:16px 20px; }
.msg-loading { display:flex; justify-content:center; padding:16px; }
.msg-notice { text-align:center; padding:8px; color:#999; font-size:12px; }
.reply-bar { display:flex; justify-content:space-between; align-items:center;
  padding:6px 20px; background:#f0f9eb; border-top:1px solid #d9f0d1; font-size:13px; }
.reply-label { color:#666; }
.mention-dropdown { position:absolute; bottom:140px; left:20px;
  background:#fff; border:1px solid #e0e0e0; border-radius:4px; max-height:160px; overflow-y:auto; z-index:10;
  box-shadow:0 2px 8px rgba(0,0,0,0.1); }
.mention-item { padding:8px 16px; cursor:pointer; font-size:13px; }
.mention-item:hover { background:#f0f0f0; }
.input-area { position:relative; padding:12px 20px; background:#f5f5f5; border-top:1px solid #e0e0e0; flex-shrink:0; }
.input-toolbar { display:flex; gap:4px; margin-bottom:6px; }
.input-footer { display:flex; justify-content:flex-end; margin-top:8px; }
.emoji-grid { display:grid; grid-template-columns:repeat(5,32px); gap:4px; max-width:180px; }
.emoji-item { font-size:22px; cursor:pointer; text-align:center; padding:2px; border-radius:4px; }
.emoji-item:hover { background:#f0f0f0; }
</style>
