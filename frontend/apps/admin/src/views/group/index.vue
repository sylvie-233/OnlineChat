<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '@/api'

const groups = ref<any[]>([])

onMounted(async () => {
  const { data } = await api.get('/api/admin/groups')
  if (data?.code === 200) groups.value = data.data
})
</script>

<template>
  <h2>群组管理</h2>
  <a-table :dataSource="groups" :columns="[
    { title: 'ID', dataIndex: 'id', key: 'id' },
    { title: '群名称', dataIndex: 'groupName', key: 'groupName' },
    { title: '群主ID', dataIndex: 'ownerId', key: 'ownerId' },
    { title: '成员数', dataIndex: 'memberCount', key: 'memberCount' },
    { title: '状态', dataIndex: 'status', key: 'status' },
    { title: '创建时间', dataIndex: 'createTime', key: 'createTime' },
  ]" rowKey="id" style="margin-top:16px" />
</template>
