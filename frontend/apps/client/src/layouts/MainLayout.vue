<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import { useNotifStore } from '@/stores/notification'
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  MessageOutlined, ContactsOutlined, TeamOutlined,
  BellOutlined, SettingOutlined, LogoutOutlined,
} from '@ant-design/icons-vue'

const auth = useAuthStore()
const chat = useChatStore()
const notif = useNotifStore()
const router = useRouter()
const route = useRoute()

const currentNav = computed(() => {
  if (route.path === '/contact') return 'contact' as const
  if (route.path === '/group') return 'group' as const
  if (route.path === '/notifications') return 'notif' as const
  if (route.path === '/settings') return 'settings' as const
  return 'chat' as const
})

onMounted(async () => {
  await chat.loadConversations()
  chat.connectWs()
  notif.loadUnreadCount()
})

function goChat() { router.push('/') }
function goContact() { router.push('/contact') }
function goGroup() { router.push('/group') }
function goNotif() { router.push('/notifications') }
function goSettings() { router.push('/settings') }

function handleLogout() {
  Modal.confirm({
    title: '退出登录',
    content: '确认退出当前账号？',
    okText: '确认退出',
    cancelText: '取消',
    onOk() {
      auth.logout()
      chat.reset()
      message.success('已退出登录')
      router.push('/login')
    },
  })
}
</script>

<template>
  <div class="main-layout">
    <!-- 左侧导航栏 60px -->
    <div class="nav-bar">
      <div class="nav-avatar" @click="goSettings">
        <a-avatar :src="auth.avatar" :size="36">{{ auth.nickname?.charAt(0) }}</a-avatar>
      </div>
      <div class="nav-icons">
        <a-tooltip title="聊天" placement="right">
          <div :class="['nav-item', { active: currentNav === 'chat' }]" @click="goChat">
            <MessageOutlined />
          </div>
        </a-tooltip>
        <a-tooltip title="联系人" placement="right">
          <div :class="['nav-item', { active: currentNav === 'contact' }]" @click="goContact">
            <ContactsOutlined />
          </div>
        </a-tooltip>
        <a-tooltip title="群组" placement="right">
          <div :class="['nav-item', { active: currentNav === 'group' }]" @click="goGroup">
            <TeamOutlined />
          </div>
        </a-tooltip>
        <a-tooltip title="通知" placement="right">
          <div :class="['nav-item', { active: currentNav === 'notif' }]" @click="goNotif" style="position:relative">
            <BellOutlined />
            <span v-if="notif.unreadCount > 0" class="nav-badge">{{ notif.unreadCount > 99 ? '99+' : notif.unreadCount }}</span>
          </div>
        </a-tooltip>
      </div>
      <div class="nav-bottom">
        <a-tooltip title="设置" placement="right">
          <div :class="['nav-item', { active: currentNav === 'settings' }]" @click="goSettings">
            <SettingOutlined />
          </div>
        </a-tooltip>
        <a-tooltip title="退出" placement="right">
          <div class="nav-item" @click="handleLogout">
            <LogoutOutlined />
          </div>
        </a-tooltip>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="content-area">
      <router-view v-slot="{ Component }">
        <transition name="page-fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </div>
  </div>
</template>

<style scoped>
.main-layout {
  display: flex;
  height: 100vh;
  background: #f5f5f5;
}
/* 左侧导航栏 */
.nav-bar {
  width: 60px;
  background: #2e2e2e;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 0;
  gap: 8px;
  flex-shrink: 0;
}
.nav-avatar {
  cursor: pointer;
  margin-bottom: 12px;
}
.nav-icons {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.nav-item {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  cursor: pointer;
  color: #999;
  font-size: 20px;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}
.nav-item::before {
  content: '';
  position: absolute;
  left: -8px;
  width: 3px;
  height: 0;
  background: #07c160;
  border-radius: 0 3px 3px 0;
  transition: height 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}
.nav-item:hover { color: #fff; background: #3a3a3a; }
.nav-item.active { color: #07c160; background: rgba(7, 193, 96, 0.12); }
.nav-item.active::before { height: 20px; }
.nav-badge {
  position:absolute; top:2px; right:2px;
  min-width:16px; height:16px; line-height:16px;
  background:#ff4d4f; color:#fff; font-size:10px;
  border-radius:8px; text-align:center; padding:0 4px;
}
.nav-bottom {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
/* 右侧内容区 */
.content-area {
  flex: 1;
  display: flex;
  overflow: hidden;
}
/* 页面切换动画 */
.page-fade-enter-active, .page-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.page-fade-enter-from {
  opacity: 0;
  transform: translateX(12px);
}
.page-fade-leave-to {
  opacity: 0;
  transform: translateX(-12px);
}
</style>
