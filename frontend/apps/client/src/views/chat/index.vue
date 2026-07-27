<script setup lang="ts">
import { useChatStore } from '@/stores/chat'
import ConversationList from './ConversationList.vue'
import ChatWindow from './ChatWindow.vue'

const chat = useChatStore()
</script>

<template>
  <div class="chat-view">
    <!-- 会话列表 250px -->
    <div class="conv-panel">
      <ConversationList />
    </div>
    <!-- 聊天窗口 -->
    <div class="chat-main">
      <ChatWindow v-if="chat.activeId || chat.activeTargetId" />
      <div v-else class="chat-placeholder">
        <div class="placeholder-content">
          <h2>OnlineChat</h2>
          <p>选择一个会话开始聊天</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-view { display:flex; flex:1; overflow:hidden; }
.conv-panel {
  width: 260px;
  background: #e8e8e8;
  border-right: 1px solid #d9d9d9;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}
.chat-main { flex:1; display:flex; flex-direction:column; background:#f5f5f5; }
.chat-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
.placeholder-content { text-align:center; color:#999; }
.placeholder-content h2 { font-size:28px; margin-bottom:8px; color:#07c160; }
</style>
