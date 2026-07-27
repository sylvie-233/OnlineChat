<script setup lang="ts">
import http from '@/api'

const groups = ref<any[]>([])
const loading = ref(false)
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

async function loadGroups() {
  loading.value = true
  try {
    // 简化: 直接查群组列表; 后端可加 /api/admin/groups 端点
    const { data } = await http.get('/api/admin/stats')
    if (data?.code === 200) {
      // stats 只返回数量; 群组详情需额外端点
      // 这里展示基础统计
    }
  } catch (e) { /* ignore */ }
  loading.value = false
}

onMounted(loadGroups)
</script>

<template>
  <div>
    <h2>群组管理</h2>
    <a-empty description="群组详细管理功能开发中" style="margin-top:60px" />
  </div>
</template>
