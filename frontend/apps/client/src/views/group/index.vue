<script setup lang="ts">
import { message } from 'ant-design-vue'
import { useGroupStore } from '@/stores/group'
import { groupApi } from '@/api/group'
import { useChatStore } from '@/stores/chat'
import { useRouter } from 'vue-router'
import { PlusOutlined, TeamOutlined } from '@ant-design/icons-vue'

const tab = ref('my')
const group = useGroupStore()
const chat = useChatStore()
const router = useRouter()

const showCreate = ref(false)
const newGroupName = ref('')
const searchGroupId = ref('')

onMounted(() => {
  group.loadMyGroups()
  group.loadInvitations()
})

async function createGroup() {
  if (!newGroupName.value.trim()) return
  await group.createGroup(newGroupName.value.trim())
  showCreate.value = false
  newGroupName.value = ''
  message.success('群创建成功')
}

async function joinGroup() {
  const id = Number(searchGroupId.value)
  if (!id) return
  await groupApi.join(id)
  message.success('已加入群')
  searchGroupId.value = ''
}

async function openGroupChat(groupInfo: any) {
  await group.openGroup(groupInfo.id)
  const conv = {
    id: groupInfo.id,
    type: 1,
    targetId: groupInfo.id,
    targetName: groupInfo.groupName,
    unreadCount: 0,
    isPinned: 0,
    isMuted: 0,
  } as any
  chat.openConversation(conv)
  router.push('/')
}
</script>

<template>
  <div class="group-view">
    <div class="group-left">
      <div class="group-header">
        <a-tabs v-model:activeKey="tab" size="small">
          <a-tab-pane key="my" tab="我的群" />
          <a-tab-pane key="join" tab="加入群" />
        </a-tabs>
        <a-button type="text" size="small" @click="showCreate = true">
          <PlusOutlined /> 创建群
        </a-button>
      </div>

      <div class="group-list">
        <div v-for="g in group.myGroups" :key="g.id" class="group-item" @click="openGroupChat(g)">
          <a-avatar :size="40" :src="g.avatar">
            <TeamOutlined />
          </a-avatar>
          <div class="group-info">
            <div class="group-name">{{ g.groupName }}</div>
            <div class="group-meta">{{ g.memberCount }} 成员</div>
          </div>
        </div>

        <div v-if="tab === 'join'" class="join-section">
          <a-input-search
            v-model:value="searchGroupId"
            placeholder="输入群 ID"
            @search="joinGroup"
          />
        </div>
      </div>
    </div>

    <!-- 群详情 -->
    <div v-if="group.currentGroup" class="group-detail">
      <h3>{{ group.currentGroup.groupName }}</h3>
      <a-tabs size="small">
        <a-tab-pane key="members" :tab="`成员(${group.members.length})`">
          <div v-for="m in group.members" :key="m.id" class="member-item">
            <a-avatar :size="32">M</a-avatar>
            <div class="member-info">
              <div>{{ m.nicknameInGroup || `用户${m.userId}` }}</div>
              <a-tag v-if="m.role === 2" color="red">群主</a-tag>
              <a-tag v-else-if="m.role === 1" color="blue">管理员</a-tag>
            </div>
          </div>
        </a-tab-pane>
        <a-tab-pane key="announce" tab="公告">
          <div v-for="a in group.announcements" :key="a.id" class="announce-item">
            <div class="announce-title">{{ a.title }}</div>
            <div class="announce-content">{{ a.content }}</div>
            <div class="announce-time">{{ a.createTime }}</div>
          </div>
        </a-tab-pane>
      </a-tabs>
    </div>

    <!-- 创建群弹窗 -->
    <a-modal v-model:open="showCreate" title="创建群聊" @ok="createGroup">
      <a-input v-model:value="newGroupName" placeholder="群名称" />
    </a-modal>
  </div>
</template>

<style scoped>
.group-view { display:flex; flex:1; overflow:hidden; }
.group-left { width:300px; background:#fff; border-right:1px solid #f0f0f0; display:flex; flex-direction:column; flex-shrink:0; }
.group-header { padding:8px 12px; display:flex; align-items:center; justify-content:space-between; }
.group-list { flex:1; overflow-y:auto; padding:8px; }
.group-item { display:flex; align-items:center; gap:10px; padding:10px; cursor:pointer; border-radius:6px; }
.group-item:hover { background:#f5f5f5; }
.group-info { flex:1; }
.group-name { font-size:14px; font-weight:500; }
.group-meta { font-size:12px; color:#999; }
.join-section { padding:12px 0; }
.group-detail { flex:1; padding:20px; overflow-y:auto; background:#f5f5f5; }
.member-item { display:flex; align-items:center; gap:8px; padding:8px 0; border-bottom:1px solid #f0f0f0; }
.member-info { display:flex; align-items:center; gap:8px; }
.announce-item { padding:12px; background:#fff; border-radius:6px; margin-bottom:8px; }
.announce-title { font-weight:600; margin-bottom:4px; }
.announce-content { font-size:13px; color:#666; }
.announce-time { font-size:11px; color:#999; margin-top:4px; }
</style>
