<script setup lang="ts">
import { useNotifStore } from '@/stores/notification'
import { BellOutlined, CheckOutlined } from '@ant-design/icons-vue'

const notif = useNotifStore()

onMounted(() => notif.loadList())

const typeLabels: Record<number, string> = {
  0: '系统通知', 1: '好友申请', 2: '群邀请', 3: '@提醒', 4: '会话消息',
}
</script>

<template>
  <div class="notif-view">
    <div class="notif-header">
      <h3>
        <BellOutlined />
        通知中心
      </h3>
      <a-button size="small" @click="notif.markAllRead" v-if="notif.unreadCount > 0">
        <CheckOutlined /> 全部已读
      </a-button>
    </div>
    <div class="notif-list">
      <div
        v-for="n in notif.list"
        :key="n.id"
        :class="['notif-item', { unread: n.isRead === 0 }]"
        @click="notif.markRead(n.id)"
      >
        <div class="notif-icon">
          <a-tag :color="n.type === 1 ? 'blue' : n.type === 3 ? 'red' : 'green'">
            {{ typeLabels[n.type] || '通知' }}
          </a-tag>
        </div>
        <div class="notif-body">
          <div class="notif-title">{{ n.title }}</div>
          <div class="notif-content">{{ n.content }}</div>
          <div class="notif-time">{{ n.createTime }}</div>
        </div>
        <a-badge v-if="n.isRead === 0" status="processing" />
      </div>
      <a-empty v-if="notif.list.length === 0" description="暂无通知" />
    </div>
  </div>
</template>

<style scoped>
.notif-view { flex:1; display:flex; flex-direction:column; background:#fff; }
.notif-header { display:flex; justify-content:space-between; align-items:center; padding:16px 20px; border-bottom:1px solid #f0f0f0; }
.notif-list { flex:1; overflow-y:auto; }
.notif-item {
  display:flex; align-items:flex-start; gap:12px; padding:14px 20px;
  border-bottom:1px solid #f5f5f5; cursor:pointer; transition:background 0.15s;
}
.notif-item:hover { background:#fafafa; }
.notif-item.unread { background:#f0f9eb; }
.notif-icon { flex-shrink:0; margin-top:2px; }
.notif-body { flex:1; min-width:0; }
.notif-title { font-size:14px; font-weight:500; margin-bottom:2px; }
.notif-content { font-size:12px; color:#666; }
.notif-time { font-size:11px; color:#999; margin-top:4px; }
</style>
