<script setup lang="ts">
import { reactive, ref, nextTick, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

interface Message { id: number; fromUserId: number; content: string; sendTime: string }

const messages = ref<Message[]>([])
const input = ref('')
const chatBox = ref<HTMLDivElement>()

function send() {
  if (!input.value.trim()) return
  messages.value.push({
    id: Date.now(),
    fromUserId: auth.userId,
    content: input.value,
    sendTime: new Date().toLocaleTimeString(),
  })
  input.value = ''
  nextTick(() => {
    chatBox.value?.scrollTo({ top: chatBox.value.scrollHeight, behavior: 'smooth' })
  })
}

onMounted(() => {
  // TODO: 加载历史消息 & 建立 WebSocket 连接
})
</script>

<template>
  <div class="chat-page">
    <div class="chat-header">
      <h3>消息</h3>
      <a-button type="link" @click="auth.logout()">退出</a-button>
    </div>
    <div class="chat-body" ref="chatBox">
      <div v-for="msg in messages" :key="msg.id"
           :class="['msg', msg.fromUserId === auth.userId ? 'msg-mine' : 'msg-other']">
        <div class="msg-bubble">{{ msg.content }}</div>
        <div class="msg-time">{{ msg.sendTime }}</div>
      </div>
    </div>
    <div class="chat-footer">
      <a-textarea v-model:value="input" :rows="3" placeholder="输入消息..." @press-enter="send" />
      <a-button type="primary" @click="send" style="margin-left:8px">发送</a-button>
    </div>
  </div>
</template>

<style scoped>
.chat-page { display:flex; flex-direction:column; height:100vh; }
.chat-header { display:flex; justify-content:space-between; align-items:center;
  padding:12px 20px; border-bottom:1px solid #f0f0f0; }
.chat-body { flex:1; overflow-y:auto; padding:20px; }
.chat-footer { display:flex; padding:12px 20px; border-top:1px solid #f0f0f0; align-items:flex-end; }
.msg { margin-bottom:16px; }
.msg-mine { text-align:right; }
.msg-bubble { display:inline-block; padding:10px 16px; border-radius:12px; max-width:70%; word-break:break-word; }
.msg-mine .msg-bubble { background:#1677ff; color:#fff; }
.msg-other .msg-bubble { background:#f0f0f0; color:#333; }
.msg-time { font-size:12px; color:#999; margin-top:4px; }
</style>
