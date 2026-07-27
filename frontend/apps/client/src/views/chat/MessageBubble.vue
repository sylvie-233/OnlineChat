<script setup lang="ts">
import { message as msgApi } from 'ant-design-vue'
import type { Message } from '@/types'
import { chatApi } from '@/api/chat'
import { RollbackOutlined, CopyOutlined, EyeOutlined } from '@ant-design/icons-vue'

const props = defineProps<{ message: Message; isMine: boolean }>()
const emit = defineEmits<{ recall: []; reply: [] }>()
const showMenu = ref(false)
const showReactions = ref(false)
const previewVisible = ref(false)
const previewImage = ref('')

function formatTime(time: string) {
  if (!time) return ''
  return new Date(time).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

// 判断消息类型渲染
const msgRender = computed(() => {
  const t = props.message.msgType
  const c = props.message.content || ''
  if (t === 1) return { type: 'image', src: c }
  if (t === 4) return { type: 'file', name: extractFileName(c), url: c }
  if (t === 5) return { type: 'location', text: '📍 位置信息' }
  if (t === 6) return { type: 'link', text: c }
  if (t === 3) return { type: 'video', text: '🎬 视频消息' }
  if (t === 2) return { type: 'voice', text: '🎤 语音消息' }
  return { type: 'text', text: c }
})

function extractFileName(content: string) {
  const match = content.match(/\[文件\] (.+)/)
  return match ? match[1].split('\n')[0] : '未知文件'
}

function openImagePreview(src: string) {
  previewImage.value = src
  previewVisible.value = true
}

function openFileUrl(url: string) {
  globalThis.open(url, '_blank')
}

function copyText(text: string) {
  navigator.clipboard.writeText(text).then(() => msgApi.success('已复制'))
}

async function handleReaction(emoji: string) {
  try {
    await chatApi.addReaction(props.message.id, emoji)
    msgApi.success('已添加 ' + emoji)
  } catch (e) { /* ignore */ }
}

async function loadReactions() {
  try {
    const { data } = await chatApi.getReactions(props.message.id)
    if (data.code === 200) showReactions.value = true
  } catch (e) { /* ignore */ }
}
</script>

<template>
  <div :class="['msg-row', isMine ? 'msg-mine' : 'msg-other']">
    <!-- 发送者头像（非自己的消息） -->
    <a-avatar v-if="!isMine" :size="32" class="msg-avatar">
      {{ (message.fromNickname || '?').charAt(0) }}
    </a-avatar>

    <div class="msg-body">
      <!-- 发送者昵称 -->
      <div v-if="!isMine && message.fromNickname" class="msg-sender">
        {{ message.fromNickname }}
      </div>

      <!-- 气泡 -->
      <a-dropdown :trigger="['contextmenu']">
        <div :class="['msg-bubble', isMine ? 'bubble-mine' : 'bubble-other']">
          <!-- 回复引用 -->
          <div v-if="message.replyToMsgId" class="msg-reply">
            <span>↩ 引用消息</span>
          </div>

          <!-- 按类型渲染 -->
          <!-- 文本 -->
          <div v-if="msgRender.type === 'text'" class="msg-content">{{ message.content }}</div>

          <!-- 图片 -->
          <div v-else-if="msgRender.type === 'image'" class="msg-image">
            <img v-if="msgRender.src" :src="msgRender.src" @click="openImagePreview(msgRender.src!)" loading="lazy"
                 style="max-width:240px;max-height:240px;border-radius:4px;cursor:pointer" />
          </div>

          <!-- 文件 -->
          <div v-else-if="msgRender.type === 'file'" class="msg-file">
            <a-button type="link" @click="openFileUrl(msgRender.url!)">
              📎 {{ msgRender.name }}
            </a-button>
          </div>

          <!-- 其他类型 -->
          <div v-else class="msg-content">{{ msgRender.text }}</div>

          <!-- 已读/发送状态 -->
          <div v-if="isMine" class="msg-status">
            <span v-if="message.status === 1" style="color:#999">✓</span>
            <span v-else-if="message.status >= 3" style="color:#07c160">✓✓</span>
          </div>
        </div>

        <template #overlay>
          <a-menu>
            <a-menu-item @click="emit('reply')">
              <span>↩ 回复</span>
            </a-menu-item>
            <a-menu-item @click="copyText(message.content)">
              <CopyOutlined /> 复制
            </a-menu-item>
            <a-menu-item @click="handleReaction('👍')">👍</a-menu-item>
            <a-menu-item @click="handleReaction('❤️')">❤️</a-menu-item>
            <a-menu-item @click="handleReaction('😂')">😂</a-menu-item>
            <a-menu-item v-if="isMine && !message.isRecalled" @click="emit('recall')">
              <RollbackOutlined /> 撤回
            </a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>
    </div>

    <!-- 时间 -->
    <span class="msg-time">{{ formatTime(message.sendTime || message.createTime) }}</span>

    <!-- 图片预览弹窗 -->
    <a-modal v-model:open="previewVisible" :footer="null" width="auto" centered>
      <img :src="previewImage" style="max-width:80vw;max-height:80vh" />
    </a-modal>
  </div>
</template>

<style scoped>
.msg-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 14px;
  gap: 8px;
}
.msg-mine { flex-direction: row-reverse; }
.msg-other { flex-direction: row; }
.msg-avatar { flex-shrink: 0; }

.msg-body { max-width: 55%; }
.msg-sender { font-size: 11px; color: #999; margin-bottom: 2px; padding-left: 4px; }

.msg-bubble {
  padding: 9px 12px;
  border-radius: 4px;
  position: relative;
  word-break: break-word;
  cursor: default;
  display: inline-block;
}
.bubble-mine { background: #95ec69; }
.bubble-other { background: #fff; border: 1px solid #f0f0f0; }
.msg-content { font-size: 14px; line-height: 1.5; white-space: pre-wrap; }
.msg-reply { font-size: 12px; color: #07c160; margin-bottom: 4px; padding-left: 6px; border-left: 3px solid #07c160; }
.msg-status { font-size: 11px; margin-top: 2px; text-align: right; }
.msg-image { line-height: 0; }
.msg-image img { transition: opacity 0.2s; }
.msg-image img:hover { opacity: 0.9; }
.msg-time { font-size: 11px; color: #b0b0b0; flex-shrink: 0; align-self: flex-end; }
.msg-file { padding: 4px 0; }
</style>
