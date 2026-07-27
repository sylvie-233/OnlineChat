<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '@/api'

const stats = ref({ users: 0, groups: 0, onlineUsers: 0 })

onMounted(async () => {
  // TODO: 接入后端统计 API
  const { data } = await api.get('/api/admin/stats').catch(() => ({ data: null }))
  if (data?.code === 200) stats.value = data.data
})
</script>

<template>
  <h2>仪表盘</h2>
  <a-row :gutter="16" style="margin-top:24px">
    <a-col :span="8">
      <a-card><a-statistic title="用户总数" :value="stats.users" /></a-card>
    </a-col>
    <a-col :span="8">
      <a-card><a-statistic title="群组总数" :value="stats.groups" /></a-card>
    </a-col>
    <a-col :span="8">
      <a-card><a-statistic title="在线用户" :value="stats.onlineUsers" /></a-card>
    </a-col>
  </a-row>
</template>
