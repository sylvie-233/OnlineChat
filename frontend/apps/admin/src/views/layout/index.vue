<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { useAdminAuthStore } from '@/stores/auth'
import {
  DashboardOutlined, UserOutlined, LogoutOutlined,
} from '@ant-design/icons-vue'

const router = useRouter()
const route = useRoute()
const auth = useAdminAuthStore()
const collapsed = ref(false)

const selectedKeys = computed(() => {
  if (route.path === '/users') return ['users']
  if (route.path === '/groups') return ['groups']
  return ['dashboard']
})

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <a-layout style="min-height:100vh">
    <a-layout-sider v-model:collapsed="collapsed" collapsible>
      <div class="logo">OC Admin</div>
      <a-menu theme="dark" mode="inline" :selectedKeys="selectedKeys">
        <a-menu-item key="dashboard" @click="router.push('/')">
          <DashboardOutlined /> 仪表盘
        </a-menu-item>
        <a-menu-item key="users" @click="router.push('/users')">
          <UserOutlined /> 用户管理
        </a-menu-item>
        <a-menu-item key="groups" @click="router.push('/groups')">
          <TeamOutlined /> 群组管理
        </a-menu-item>
      </a-menu>
    </a-layout-sider>
    <a-layout>
      <a-layout-header class="header">
        <span>{{ auth.nickname || '管理员' }}</span>
        <a-button type="link" @click="handleLogout">
          <LogoutOutlined /> 退出
        </a-button>
      </a-layout-header>
      <a-layout-content class="content">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<style scoped>
.logo { height:64px; display:flex; align-items:center; justify-content:center;
  color:#fff; font-size:18px; font-weight:bold; }
.header { background:#fff; display:flex; justify-content:flex-end; align-items:center; padding:0 24px; }
.content { padding:24px; background:#f0f2f5; }
</style>
