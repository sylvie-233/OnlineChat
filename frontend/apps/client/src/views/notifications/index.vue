<script setup lang="ts">
import { useNotifStore } from '@/stores/notification'
import { useChatStore } from '@/stores/chat'
import { useRouter } from 'vue-router'
import {
  BellOutlined, CheckOutlined, UserAddOutlined,
  MessageOutlined, TeamOutlined, InfoCircleOutlined,
  SearchOutlined, LeftOutlined, RightOutlined, AimOutlined, MailOutlined,
} from '@ant-design/icons-vue'

const notif = useNotifStore()
const chat = useChatStore()
const router = useRouter()

onMounted(() => notif.loadList())

const typeConfig: Record<number, { label: string; icon: any; color: string; bg: string }> = {
  0: { label: '系统',  icon: InfoCircleOutlined, color: '#1677ff', bg: '#e6f4ff' },
  1: { label: '好友',  icon: UserAddOutlined,    color: '#52c41a', bg: '#f6ffed' },
  2: { label: '群组',  icon: TeamOutlined,       color: '#722ed1', bg: '#f9f0ff' },
  3: { label: '@我',   icon: AimOutlined,        color: '#ff4d4f', bg: '#fff2f0' },
  4: { label: '消息',  icon: MailOutlined,       color: '#fa8c16', bg: '#fff7e6' },
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
  if (n.type === 4) router.push('/')
}

function formatTime(time: string) {
  if (!time) return ''
  return new Date(time).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const totalPages = computed(() => Math.ceil(notif.total / notif.pageSize) || 1)
const pages = computed(() => {
  const p: number[] = []
  const total = totalPages.value
  const cur = notif.currentPage
  let start = Math.max(1, cur - 2)
  let end = Math.min(total, cur + 2)
  if (end - start < 4) {
    if (start === 1) end = Math.min(total, start + 4)
    else start = Math.max(1, end - 4)
  }
  for (let i = start; i <= end; i++) p.push(i)
  return p
})
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

    <!-- 搜索+筛选栏 -->
    <div class="notif-toolbar">
      <a-input v-model:value="notif.pendingKeyword" placeholder="搜索通知" style="width:220px"
        @press-enter="notif.doSearch()" />
      <a-select v-model:value="notif.pendingIsRead" style="width:110px" allow-clear placeholder="全部状态">
        <a-select-option :value="0">未读</a-select-option>
        <a-select-option :value="1">已读</a-select-option>
      </a-select>
      <a-button type="primary" size="small" @click="notif.doSearch()" :loading="notif.loading">
        <SearchOutlined /> 搜索
      </a-button>
      <a-button size="small" @click="notif.doReset()">重置</a-button>
    </div>

    <!-- 列表 -->
    <div class="notif-list" v-if="notif.list.length > 0">
      <a-spin :spinning="notif.loading">
        <div v-for="group in groupedNotifications" :key="group.label" class="notif-group">
          <div class="group-label">{{ group.label }}</div>
          <div
            v-for="n in group.items"
            :key="n.id"
            :class="['notif-item', { unread: !n.isRead }]"
            @click="handleClick(n)"
          >
            <div class="notif-icon" :style="{ background: (typeConfig[n.type] || typeConfig[0]).bg }">
              <component :is="(typeConfig[n.type] || typeConfig[0]).icon" />
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
      </a-spin>
    </div>

    <div v-else class="notif-empty">
      <BellOutlined class="empty-icon" />
      <p>暂无通知</p>
    </div>

    <!-- 分页 -->
    <div class="notif-pager">
      <a-button :disabled="notif.currentPage <= 1" size="small" @click="notif.loadList(notif.currentPage - 1)">
        <LeftOutlined />
      </a-button>
      <a-button v-for="p in pages" :key="p"
        :type="p === notif.currentPage ? 'primary' : 'default'"
        size="small" @click="notif.loadList(p)">{{ p }}</a-button>
      <a-button :disabled="notif.currentPage >= totalPages" size="small"
        @click="notif.loadList(notif.currentPage + 1)">
        <RightOutlined />
      </a-button>
      <span class="pager-total">共 {{ notif.total }} 条</span>
      <a-select :value="notif.pageSize" size="small" style="width:90px;margin-left:12px"
        @change="notif.setPageSize($event as number)">
        <a-select-option :value="10">10条/页</a-select-option>
        <a-select-option :value="20">20条/页</a-select-option>
        <a-select-option :value="50">50条/页</a-select-option>
      </a-select>
    </div>
  </div>
</template>

<style scoped>
.notif-view { flex:1; display:flex; flex-direction:column; background:#fff; }
.notif-header {
  display:flex; justify-content:space-between; align-items:center;
  padding:16px 20px 0; flex-shrink:0;
}
.notif-title-row { display:flex; align-items:center; gap:10px; }
.notif-title-row h3 { margin:0; font-size:16px; }
.notif-title-icon { font-size:18px; color:#1677ff; }

.notif-toolbar { padding: 12px 20px 0; display:flex; gap:8px; align-items:center; flex-shrink:0; }

.notif-list { flex:1; overflow-y:auto; padding:8px 20px; }
.notif-group { margin-bottom: 4px; }
.group-label { font-size:12px; color:#999; padding:12px 0 8px; }
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
  flex-shrink:0; font-size:18px; color:#fff;
}
.notif-icon :deep(svg) { fill:#fff; }
.notif-body { flex:1; min-width:0; }
.notif-top { display:flex; justify-content:space-between; align-items:center; margin-bottom:3px; }
.notif-title { font-size:14px; font-weight:500; }
.notif-tag { font-size:11px; flex-shrink:0; margin-left:8px; }
.notif-content { font-size:12px; color:#666; line-height:1.4; margin-bottom:4px; }
.notif-time { font-size:11px; color:#bbb; }
.unread-dot { width:7px; height:7px; border-radius:50%; background:#1677ff; flex-shrink:0; margin-top:8px; }

.notif-empty {
  flex:1; display:flex; flex-direction:column;
  align-items:center; justify-content:center; gap:8px; color:#bbb;
}
.notif-empty .empty-icon { font-size:48px; color:#ddd; }
.notif-empty p { font-size:15px; color:#999; margin:0; }

.notif-pager {
  display:flex; align-items:center; justify-content:center; gap:4px;
  padding:12px 20px 16px; flex-shrink:0; border-top:1px solid #f0f0f0;
}
.pager-total { font-size:12px; color:#999; margin-left:12px; }
</style>
