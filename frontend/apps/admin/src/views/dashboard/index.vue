<script setup lang="ts">
import http from '@/api'
import { UserOutlined, TeamOutlined, WifiOutlined } from '@ant-design/icons-vue'

const stats = reactive({ userCount: 0, groupCount: 0, onlineCount: 0 })
const queueStats = reactive({ enabled: false, queueSize: 0 })
const loading = ref(true)

onMounted(async () => {
  try {
    const [s, q] = await Promise.all([
      http.get('/api/admin/stats'),
      http.get('/api/admin/queue-stats'),
    ])
    if (s.data?.code === 200) Object.assign(stats, s.data.data)
    if (q.data?.code === 200) Object.assign(queueStats, q.data.data)
  } catch (e) { /* ignore */ }
  loading.value = false
})
</script>

<template>
  <div>
    <h2>仪表盘</h2>
    <a-spin :spinning="loading">
      <a-row :gutter="16" style="margin-top:24px">
        <a-col :span="6">
          <a-card><a-statistic title="用户总数" :value="stats.userCount"><UserOutlined /></a-statistic></a-card>
        </a-col>
        <a-col :span="6">
          <a-card><a-statistic title="群组总数" :value="stats.groupCount"><TeamOutlined /></a-statistic></a-card>
        </a-col>
        <a-col :span="6">
          <a-card><a-statistic title="在线用户" :value="stats.onlineCount" :value-style="{color:'#07c160'}"><WifiOutlined /></a-statistic></a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <a-statistic
              title="消息队列"
              :value="queueStats.queueSize"
              suffix="条待处理"
              :value-style="{color: queueStats.enabled ? '#07c160' : '#999'}"
            />
          </a-card>
        </a-col>
      </a-row>
    </a-spin>
  </div>
</template>
