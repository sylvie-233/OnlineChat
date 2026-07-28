<script setup lang="ts">
import { message } from 'ant-design-vue'
import { userApi } from '@/api/user'
import { DesktopOutlined, LaptopOutlined, MobileOutlined } from '@ant-design/icons-vue'

const sessions = ref<any[]>([])
const loading = ref(false)

const deviceIcon: Record<string, any> = {
  web: LaptopOutlined, desktop: DesktopOutlined,
  ios: MobileOutlined, android: MobileOutlined,
}

onMounted(loadSessions)

async function loadSessions() {
  loading.value = true
  try {
    const { data } = await userApi.getSessions()
    if (data.code === 200) sessions.value = data.data || []
  } catch (e) { /* ignore */ }
  loading.value = false
}

async function kickSession(sessionId: number) {
  try {
    await userApi.kickSession(sessionId)
    message.success('已登出该设备')
    loadSessions()
  } catch (e) { /* ignore */ }
}
</script>

<template>
  <a-spin :spinning="loading">
    <div class="session-list" v-if="sessions.length">
      <div v-for="s in sessions" :key="s.id" class="session-item">
        <component :is="deviceIcon[s.deviceType] || LaptopOutlined" style="font-size:24px" />
        <div class="session-info">
          <div>{{ s.deviceName || s.deviceType || '未知设备' }}</div>
          <div style="font-size:11px;color:#999">
            {{ s.clientIp || '' }} · 活跃: {{ s.lastActiveTime }}
          </div>
        </div>
        <a-popconfirm title="确认登出该设备？" @confirm="kickSession(s.id)" okText="确认" cancelText="取消">
          <a-button size="small" danger>登出</a-button>
        </a-popconfirm>
      </div>
    </div>
    <a-empty v-else description="暂无活跃会话" />
  </a-spin>
</template>

<style scoped>
.session-list { max-width:500px; }
.session-item { display:flex; align-items:center; gap:12px; padding:12px; border-bottom:1px solid #f0f0f0; }
.session-info { flex:1; }
</style>
