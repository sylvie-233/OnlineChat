<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '@/api'

const users = ref<any[]>([])

onMounted(async () => {
  const { data } = await api.get('/api/admin/users')
  if (data?.code === 200) users.value = data.data
})
</script>

<template>
  <h2>用户管理</h2>
  <a-table :dataSource="users" :columns="[
    { title: 'ID', dataIndex: 'id', key: 'id' },
    { title: '用户名', dataIndex: 'username', key: 'username' },
    { title: '昵称', dataIndex: 'nickname', key: 'nickname' },
    { title: '状态', dataIndex: 'status', key: 'status' },
    { title: '注册时间', dataIndex: 'createTime', key: 'createTime' },
  ]" rowKey="id" style="margin-top:16px" />
</template>
