<script setup lang="ts">
import { useChatStore } from '@/stores/chat'
import { SearchOutlined } from '@ant-design/icons-vue'

const chat = useChatStore()
const searchText = ref('')
const showAddMenu = ref(false)

const filteredConvs = computed(() => {
  if (!searchText.value) return chat.conversations
  const kw = searchText.value.toLowerCase()
  return chat.conversations.filter((c) =>
    (c.targetName || '').toLowerCase().includes(kw),
  )
})

function selectConv(conv: any) {
  chat.openConversation(conv)
}

function formatTime(time: string) {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  if (d.toDateString() === now.toDateString()) {
    return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}
</script>

<template>
  <div class="conv-list">
    <!-- 搜索栏 -->
    <div class="conv-header">
      <a-input
        v-model:value="searchText"
        placeholder="搜索"
        size="small"
        allow-clear
      >
        <template #prefix><SearchOutlined /></template>
      </a-input>
    </div>

    <!-- 会话列表 -->
    <div class="conv-items">
      <div
        v-for="conv in filteredConvs"
        :key="conv.id"
        :class="['conv-item', { active: conv.id === chat.activeId }]"
        @click="selectConv(conv)"
      >
        <div class="conv-avatar">
          <a-badge :count="conv.unreadCount" :overflow-count="99" :dot="conv.isMuted === 1">
            <a-avatar :size="40" :src="conv.targetAvatar">
              {{ (conv.targetName || '?').charAt(0) }}
            </a-avatar>
          </a-badge>
        </div>
        <div class="conv-info">
          <div class="conv-top">
            <span class="conv-name">{{ conv.targetName || `用户${conv.targetId}` }}</span>
            <span class="conv-time">{{ formatTime(conv.updateTime) }}</span>
          </div>
          <div class="conv-bottom">
            <span class="conv-preview">{{ conv.lastContent || '' }}</span>
            <span v-if="conv.isMuted" class="conv-muted">🔇</span>
          </div>
        </div>
      </div>

      <a-empty v-if="filteredConvs.length === 0" description="暂无会话" />
    </div>
  </div>
</template>

<style scoped>
.conv-list { display:flex; flex-direction:column; height:100%; }
.conv-header { padding:12px; }
.conv-items { flex:1; overflow-y:auto; }
.conv-item {
  display: flex;
  padding: 12px;
  cursor: pointer;
  transition: background 0.15s;
  align-items: center;
  gap: 10px;
}
.conv-item:hover { background: #dcdcdc; }
.conv-item.active { background: #c8c8c8; }
.conv-avatar { flex-shrink: 0; }
.conv-info { flex:1; min-width:0; }
.conv-top { display:flex; justify-content:space-between; margin-bottom:4px; }
.conv-name { font-size:14px; font-weight:500; }
.conv-time { font-size:11px; color:#999; }
.conv-bottom { display:flex; justify-content:space-between; }
.conv-preview { font-size:12px; color:#999; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; max-width:160px; }
.conv-muted { font-size:12px; }
</style>
