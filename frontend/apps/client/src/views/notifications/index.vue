<script setup lang="ts">
import { useNotifStore } from '@/stores/notification'
import { useChatStore } from '@/stores/chat'
import { useRouter } from 'vue-router'
import {
  BellOutlined, CheckOutlined, UserSwitchOutlined,
  MessageOutlined, TeamOutlined, InfoCircleOutlined,
} from '@ant-design/icons-vue'

const notif = useNotifStore()
const chat = useChatStore()
const router = useRouter()

onMounted(() => notif.loadList())

// 类型配置
const typeConfig: Record<number, { label: string; icon: any; color: string; bg: string }> = {
  0: { label: '系统',  icon: InfoCircleOutlined, color: '#1677ff', bg: '#e6f4ff' },
  1: { label: '好友',  icon: UserSwitchOutlined, color: '#52c41a', bg: '#f6ffed' },
  2: { label: '群组',  icon: TeamOutlined,        color: '#722ed1', bg: '#f9f0ff' },
  3: { label: '@我',   icon: MessageOutlined,     color: '#ff4d4f', bg: '#fff2f0' },
  4: { label: '消息',  icon: MessageOutlined,     color: '#1677ff', bg: '#e6f4ff' },
}

// 按日期分组
const groupedNotifications = computed(() => {
  const groups: { label: string; items: typeof notif.list }[] = []
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const yesterday = new Date(today.getTime() - 86400000)

  for (const n of notif.list) {
    const d = new Date(n.createTime)
    const dateOnly = new Date(d.getFullYear(), d.getMonth(), d.getDate())
    let label: string
    if (dateOnly.getTime() === today.getTime()) label = '今天'
    else if (dateOnly.getTime() === yesterday.getTime()) label = '昨天'
    else label = d.toLocaleDateString('zh-CN', { month: 'long', day: 'numeric' })

    let group = groups.find(g => g.label === label)
    if (!group) { group = { label, items: [] }; groups.push(group) }
    group.items.push(n)
  }
  return groups
})

function handleClick(n: any) {
  if (!n.isRead) notif.markRead(n.id)
  // 消息类通知：点击跳转到对应会话
  if (n.type === 4 && n.relatedId) {
    // 简单跳转到聊天页，不传具体会话
    router.push('/')
  }
}

function formatTime(time: string) {
  if (!time) return ''
  return new Date(time).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}
</script>

<template>
  <div class="notif-view">
    <!-- 顶栏 -->
    <div class="notif-header">
      <div class="notif-title-row">
        <BellOutlined class="notif-title-icon" />
        <h3>通知中心</h3>
        <a-badge v-if="notif.unreadCount > 0" :count="notif.unreadCount" :overflow-count="99" />
      </div>
      <a-button v-if="notif.unreadCount > 0" type="link" size="small" @click="notif.markAllRead">
        <CheckOutlined /> 全部已读
      </a-button>
    </div>

    <!-- 通知列表 -->
    <div class="notif-list" v-if="notif.list.length > 0">
      <div v-for="group in groupedNotifications" :key="group.label" class="notif-group">
        <div class="group-label">{{ group.label }}</div>
        <div
          v-for="n in group.items"
          :key="n.id"
          :class="['notif-item', { unread: !n.isRead }]"
          @click="handleClick(n)"
        >
          <div
            class="notif-icon"
            :style="{ background: (typeConfig[n.type] || typeConfig[0]).bg }"
          >
            <component
              :is="(typeConfig[n.type] || typeConfig[0]).icon"
              :style="{ color: (typeConfig[n.type] || typeConfig[0]).color }"
            />
          </div>
          <div class="notif-body">
            <div class="notif-top">
              <span class="notif-title">{{ n.title }}</span>
              <span class="notif-tag" :style="{ color: (typeConfig[n.type] || typeConfig[0]).color }">
                {{ (typeConfig[n.type] || typeConfig[0]).label }}
              </span>
            </div>
            <div class="notif-content">{{ n.content }}</div>
            <div class="notif-time">{{ formatTime(n.createTime) }}</div>
          </div>
          <div v-if="!n.isRead" class="unread-dot" />
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="notif-empty">
      <BellOutlined class="empty-icon" />
      <p>暂无通知</p>
      <span>当有人给你发消息或好友申请时，这里会出现通知</span>
    </div>
  </div>
</template>

<style scoped>
.notif-view { flex:1; display:flex; flex-direction:column; background:#fff; }
.notif-header {
  display:flex; justify-content:space-between; align-items:center;
  padding:16px 20px; border-bottom:1px solid #f0f0f0;
}
.notif-title-row { display:flex; align-items:center; gap:10px; }
.notif-title-row h3 { margin:0; font-size:16px; }
.notif-title-icon { font-size:18px; color:#1677ff; }

/* 列表 */
.notif-list { flex:1; overflow-y:auto; padding:0 20px; }
.notif-group { margin-bottom: 4px; }
.group-label {
  font-size:12px; color:#999; padding:12px 0 8px;
  position:sticky; top:0; background:#fff; z-index:1;
}
.notif-item {
  display:flex; align-items:flex-start; gap:12px;
  padding:12px; border-radius:8px; cursor:pointer;
  transition:background 0.15s; position:relative;
}
.notif-item:hover { background:#fafafa; }
.notif-item.unread { background:#f0f7ff; }

.notif-icon {
  width:36px; height:36px; border-radius:50%;
  display:flex; align-items:center; justify-content:center;
  flex-shrink:0; font-size:16px;
}
.notif-body { flex:1; min-width:0; }
.notif-top { display:flex; justify-content:space-between; align-items:center; margin-bottom:3px; }
.notif-title { font-size:14px; font-weight:500; }
.notif-tag { font-size:11px; flex-shrink:0; margin-left:8px; }
.notif-content { font-size:12px; color:#666; line-height:1.4; margin-bottom:4px; }
.notif-time { font-size:11px; color:#bbb; }
.unread-dot {
  width:7px; height:7px; border-radius:50%; background:#1677ff;
  flex-shrink:0; margin-top:8px;
}

/* 空状态 */
.notif-empty {
  flex:1; display:flex; flex-direction:column;
  align-items:center; justify-content:center; gap:8px; color:#bbb;
}
.notif-empty .empty-icon { font-size:48px; color:#ddd; }
.notif-empty p { font-size:15px; color:#999; margin:0; }
.notif-empty span { font-size:12px; }
</style>
