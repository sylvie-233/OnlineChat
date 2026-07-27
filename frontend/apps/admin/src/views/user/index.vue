<script setup lang="ts">
import { message } from 'ant-design-vue'
import http from '@/api'

const users = ref<any[]>([])
const loading = ref(false)
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const keyword = ref('')

async function loadUsers() {
  loading.value = true
  try {
    const { data } = await http.get('/api/admin/users', {
      params: { page: pagination.current, size: pagination.pageSize, keyword: keyword.value || undefined },
    })
    if (data?.code === 200) {
      users.value = data.data.records || data.data || []
      pagination.total = data.data.total || 0
    }
  } catch (e) { /* ignore */ }
  loading.value = false
}

onMounted(loadUsers)

function handleTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadUsers()
}

async function banUser(userId: number, status: number) {
  await http.put(`/api/admin/users/${userId}/ban`, { status })
  message.success(status === 2 ? '已封禁' : '已解封')
  loadUsers()
}

async function muteUser(userId: number, muted: boolean) {
  await http.put(`/api/admin/users/${userId}/mute`, { muted })
  message.success(muted ? '已禁言' : '已取消禁言')
  loadUsers()
}

const statusMap: Record<number, { color: string; label: string }> = {
  0: { color: 'green', label: '正常' },
  1: { color: 'orange', label: '禁言' },
  2: { color: 'red', label: '封禁' },
}

const columns: any[] = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '用户名', dataIndex: 'username', key: 'username' },
  { title: '昵称', dataIndex: 'nickname', key: 'nickname' },
  { title: '邮箱', dataIndex: 'email', key: 'email' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '注册时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'actions', width: 200 },
]
</script>

<template>
  <div>
    <h2>用户管理</h2>
    <div style="margin-bottom:16px;display:flex;gap:12px">
      <a-input-search v-model:value="keyword" placeholder="搜索用户名/昵称" style="width:280px" @search="loadUsers" allow-clear />
    </div>
    <a-table
      :dataSource="users"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      rowKey="id"
      @change="handleTableChange"
      size="middle"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="statusMap[record.status]?.color || 'default'">
            {{ statusMap[record.status]?.label || record.status }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'actions'">
          <a-space>
            <a-button v-if="record.status === 2" size="small" @click="banUser(record.id, 0)">解封</a-button>
            <a-button v-else size="small" danger @click="banUser(record.id, 2)">封禁</a-button>
            <a-button v-if="record.status === 1" size="small" @click="muteUser(record.id, false)">取消禁言</a-button>
            <a-button v-else size="small" @click="muteUser(record.id, true)">禁言</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>
