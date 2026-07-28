<script setup lang="ts">
import { message, Modal } from 'ant-design-vue'
import { useGroupStore } from '@/stores/group'
import { useContactStore } from '@/stores/contact'
import { groupApi } from '@/api/group'
import { userApi } from '@/api/user'
import { useChatStore } from '@/stores/chat'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import AvatarCropper from '@/components/AvatarCropper.vue'
import {
  PlusOutlined, TeamOutlined, SettingOutlined,
  DeleteOutlined, PushpinOutlined, PushpinFilled,
  EditOutlined, UserAddOutlined, LogoutOutlined,
} from '@ant-design/icons-vue'

const tab = ref('my')
const detailTab = ref('members')
const group = useGroupStore()
const chat = useChatStore()
const auth = useAuthStore()
const router = useRouter()

const showCreate = ref(false)
const newGroupName = ref('')
const searchGroupId = ref('')
const searchGroupResults = ref<any[]>([])

// 公告
const showAnnounce = ref(false)
const announceTitle = ref('')
const announceContent = ref('')
const editAnnounceId = ref(0)

// 群设置
const showSettings = ref(false)
const settingsForm = ref<any>({})

// 邀请
const inviteKeyword = ref('')
const inviteResults = ref<any[]>([])
const inviteShow = ref(false)
let inviteTimer = 0

async function searchInviteUsers() {
  const kw = inviteKeyword.value.trim()
  if (!kw) { inviteResults.value = []; return }
  try {
    const { data } = await userApi.search(kw)
    if (data.code === 200) {
      // 排除已在群内的用户
      const memberIds = new Set(group.members.map(m => m.userId))
      inviteResults.value = (data.data || []).filter(u => !memberIds.has(u.id))
      inviteShow.value = inviteResults.value.length > 0
    }
  } catch { inviteResults.value = [] }
}

function onInviteInput() {
  clearTimeout(inviteTimer)
  inviteTimer = window.setTimeout(searchInviteUsers, 300)
}

async function rejectInvitation(r: any) {
  await groupApi.handleRequest(r.id, false)
  await group.loadInvitations()
}

async function doAcceptInvitation(r: any) {
  await group.acceptInvitation(r.id, r.groupId)
}

async function dismissGroup() {
  if (!group.currentGroup) return
  await groupApi.dismiss(group.currentGroup.id)
  message.success('群已解散')
  group.currentGroup = null
  await group.loadMyGroups()
}

function doInvite(user: any) {
  if (!group.currentGroup) return
  inviteShow.value = false
  const name = user.nickname || user.username
  Modal.confirm({
    title: '邀请入群',
    content: `确认邀请 ${name} 加入群「${group.currentGroup.groupName}」？`,
    okText: '确认邀请',
    cancelText: '取消',
    async onOk() {
      await groupApi.invite(group.currentGroup!.id, user.id)
      message.success(`已邀请 ${name}`)
      inviteKeyword.value = ''
    },
  })
}

// 昵称编辑
const editingNickname = ref({ userId: 0, nickname: '' })

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

async function searchGroups() {
  const kw = searchGroupId.value.trim()
  if (!kw) return
  try {
    const { data } = await groupApi.search(kw)
    if (data.code === 200) searchGroupResults.value = data.data || []
  } catch { searchGroupResults.value = [] }
}

async function doJoinGroup(g: any) {
  // joinType: 0=自由加入 1=需验证 2=禁止 (null 等同 0)
  if (g.joinType == 1) {
    await groupApi.applyJoin(g.id, '')
    message.success('已发送入群申请，等待管理员审批')
  } else {
    await groupApi.join(g.id)
    message.success('已加入群')
  }
  searchGroupResults.value = []
  await group.loadMyGroups()
}

async function selectGroup(g: any) {
  await group.openGroup(g.id)
}

async function enterGroupChat() {
  if (!group.currentGroup) return
  const g = group.currentGroup
  // 先刷新会话列表，确保群会话存在
  await chat.loadConversations()
  let conv = chat.conversations.find((c: any) => c.type === 1 && c.targetId === g.id)
  if (!conv) {
    conv = { id: 0, type: 1, targetId: g.id, targetName: g.groupName,
      unreadCount: 0, isPinned: 0, isMuted: 0 }
  }
  chat.openConversation(conv as any)
  router.push('/')
}

// 公告操作
function openPublishAnnounce() {
  announceTitle.value = ''
  announceContent.value = ''
  editAnnounceId.value = 0
  showAnnounce.value = true
}

function openEditAnnounce(a: any) {
  announceTitle.value = a.title
  announceContent.value = a.content
  editAnnounceId.value = a.id
  showAnnounce.value = true
}

async function saveAnnounce() {
  if (!group.currentGroup) return
  if (editAnnounceId.value) {
    await groupApi.updateAnnouncement(editAnnounceId.value, announceTitle.value, announceContent.value)
    message.success('公告已更新')
  } else {
    await group.publishAnnouncement(group.currentGroup.id, announceTitle.value, announceContent.value)
    message.success('公告已发布')
  }
  showAnnounce.value = false
}

// 设置
async function openSettings() {
  if (!group.currentGroup) return
  // 先刷新群信息
  const { data } = await groupApi.getById(group.currentGroup.id)
  if (data.code === 200 && data.data) group.currentGroup = data.data
  const g = group.currentGroup
  if (!g) return
  settingsForm.value = {
    groupName: g.groupName,
    avatar: g.avatar || '',
    description: g.description || '',
    joinType: g.joinType ?? 0,
    maxMembers: g.maxMembers ?? 200,
    isMutedAll: g.isMutedAll ?? 0,
  }
  showSettings.value = true
}

async function saveSettings() {
  if (!group.currentGroup) return
  await group.updateSettings(group.currentGroup.id, {
    groupName: settingsForm.value.groupName,
    avatar: settingsForm.value.avatar,
    description: settingsForm.value.description,
    joinType: settingsForm.value.joinType,
    maxMembers: settingsForm.value.maxMembers,
    isMutedAll: settingsForm.value.isMutedAll,
  })
  showSettings.value = false
  message.success('设置已更新')
}

// 邀请
async function inviteMember() {
  if (!group.currentGroup || !inviteeId.value) return
  await groupApi.invite(group.currentGroup.id, Number(inviteeId.value))
  inviteeId.value = ''
  message.success('已邀请')
}

// 角色标签
function roleTag(role: number) {
  if (role === 2) return { color: 'red', text: '群主' }
  if (role === 1) return { color: 'blue', text: '管理员' }
  return { color: 'default', text: '成员' }
}

const contactStore = useContactStore()

function getMemberDisplayName(m: any) {
  if (m.nicknameInGroup) return m.nicknameInGroup
  const user = contactStore.userCache.get(m.userId)
  return user?.nickname || user?.username || `用户${m.userId}`
}

// 切换顶层 tab 时刷新
watch(tab, (key) => {
  if (key === 'my') group.loadMyGroups()
  if (key === 'invitations') group.loadInvitations()
})
// 切换详情 tab 时刷新
watch(detailTab, (key) => {
  if (!group.currentGroup) return
  if (key === 'members') {
    groupApi.getMembers(group.currentGroup.id).then(({ data }) => {
      if (data.code === 200) group.members = data.data || []
    })
  }
  if (key === 'requests') group.loadRequests(group.currentGroup.id)
  if (key === 'announce') {
    groupApi.getAnnouncements(group.currentGroup.id).then(({ data }) => {
      if (data.code === 200) group.announcements = data.data || []
    })
  }
})

// 判断是否为群主
const isOwner = computed(() => {
  return group.currentGroup?.ownerId === auth.userId
})
// 判断是否为群主或管理员
const isAdmin = computed(() => {
  if (!group.currentGroup) return false
  return isOwner.value || group.members.some(m => m.userId === auth.userId && m.role >= 1)
})
</script>

<template>
  <div class="group-view">
    <!-- 左侧 -->
    <div class="group-left">
      <div class="group-header">
        <a-tabs v-model:activeKey="tab" size="small" style="flex:1;min-width:0">
          <a-tab-pane key="my" tab="我的群" />
          <a-tab-pane key="join" tab="加入群" />
          <a-tab-pane key="invitations" :tab="`邀请(${group.invitations.length})`" />
        </a-tabs>
      </div>

      <div class="group-list">
        <div v-if="tab === 'my'">
          <div v-for="g in group.myGroups" :key="g.id"
               :class="['group-item', { active: group.currentGroup?.id === g.id }]"
               @click="selectGroup(g)">
            <a-avatar :size="40" :src="g.avatar"><TeamOutlined /></a-avatar>
            <div class="group-info">
              <div class="group-name">{{ g.groupName }}</div>
              <div class="group-meta">{{ g.memberCount }} 成员</div>
            </div>
          </div>
          <a-button type="dashed" size="small" block @click="showCreate = true" style="margin-bottom:8px">
            <PlusOutlined /> 新建群
          </a-button>
          <a-empty v-if="group.myGroups.length === 0" description="暂无群聊" />
        </div>

        <div v-else-if="tab === 'invitations'" class="invitation-list">
          <div v-for="r in group.invitations" :key="r.id" class="request-item">
            <span>{{ group.groupNameCache.get(r.groupId) || '群' + r.groupId }} 邀请你加入</span>
            <a-popconfirm title="确认接受邀请？" @confirm="doAcceptInvitation(r)"
              okText="确认" cancelText="取消">
              <a-button size="small" type="primary">接受</a-button>
            </a-popconfirm>
            <a-popconfirm title="确认拒绝？" @confirm="rejectInvitation(r)"
              okText="确认" cancelText="取消">
              <a-button size="small">拒绝</a-button>
            </a-popconfirm>
          </div>
          <a-empty v-if="group.invitations.length === 0" description="暂无邀请" />
        </div>

        <div v-if="tab === 'join'" class="join-section">
          <a-input-search v-model:value="searchGroupId" placeholder="搜索群名称" @search="searchGroups" />
          <div v-if="searchGroupResults.length > 0" class="search-results">
            <div v-for="g in searchGroupResults" :key="g.id" class="search-result">
              <a-avatar :size="36"><TeamOutlined /></a-avatar>
              <div class="search-info">
                <div>{{ g.groupName }}</div>
                <div style="font-size:11px;color:#999">{{ g.memberCount }} 成员</div>
              </div>
              <a-tag v-if="group.myGroups.some(m => m.id === g.id)" color="green">已加入</a-tag>
              <a-button v-else size="small" type="primary" @click="doJoinGroup(g)">
                {{ g.joinType === 1 ? '申请加入' : '加入' }}
              </a-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧详情 -->
    <div v-if="group.currentGroup" class="group-detail">
      <div class="detail-header">
        <h3>{{ group.currentGroup.groupName }}</h3>
        <div class="detail-actions">
          <a-button type="primary" size="small" @click="enterGroupChat">进入聊天</a-button>
          <a-button size="small" @click="openSettings"><SettingOutlined /> 设置</a-button>
          <a-popconfirm v-if="isOwner" title="确认解散群？此操作不可恢复" @confirm="dismissGroup">
            <a-button size="small" danger><DeleteOutlined /> 解散群</a-button>
          </a-popconfirm>
          <a-button v-else size="small" @click="group.leaveGroup(group.currentGroup!.id)">
            <LogoutOutlined /> 退群
          </a-button>
        </div>
      </div>

      <a-tabs v-model:activeKey="detailTab" size="small">
        <!-- 成员 -->
        <a-tab-pane key="members" :tab="`成员(${group.members.length})`">
          <div class="member-list">
            <div v-for="m in group.members" :key="m.id" class="member-item">
              <a-avatar :size="32">M</a-avatar>
              <div class="member-name">{{ getMemberDisplayName(m) }}</div>
              <a-tag :color="roleTag(m.role).color">{{ roleTag(m.role).text }}</a-tag>
              <template v-if="isAdmin && m.userId !== auth.userId && m.role < 2">
                <a-button size="small" type="link" v-if="m.role === 0"
                  @click="group.setMemberRole(group.currentGroup!.id, m.userId, 1)">设管理员</a-button>
                <a-button size="small" type="link" v-else-if="m.role === 1"
                  @click="group.setMemberRole(group.currentGroup!.id, m.userId, 0)">取消管理员</a-button>
                <a-popconfirm title="确认踢出？" @confirm="group.kickMember(group.currentGroup!.id, m.userId)">
                  <a-button size="small" type="link" danger>踢出</a-button>
                </a-popconfirm>
              </template>
            </div>
          </div>
          <!-- 邀请 -->
          <div v-if="isAdmin" class="invite-row">
            <div class="invite-search">
              <a-input v-model:value="inviteKeyword" placeholder="搜索用户名称邀请入群"
                @input="onInviteInput" @focus="inviteKeyword && inviteResults.length && (inviteShow = true)"
                @blur="setTimeout(() => inviteShow = false, 200)">
                <template #prefix><UserAddOutlined /></template>
              </a-input>
              <div v-if="inviteShow" class="invite-dropdown">
                <div v-for="u in inviteResults" :key="u.id" class="invite-item" @mousedown="doInvite(u)">
                  <a-avatar :size="32">{{ (u.nickname || u.username).charAt(0) }}</a-avatar>
                  <div class="invite-item-info">
                    <span>{{ u.nickname }}</span>
                    <span style="font-size:11px;color:#999">@{{ u.username }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </a-tab-pane>

        <!-- 公告 -->
        <a-tab-pane key="announce" tab="公告">
          <a-button type="dashed" size="small" block @click="openPublishAnnounce" style="margin-bottom:12px"
            v-if="isAdmin">
            <PlusOutlined /> 发布公告
          </a-button>
          <div v-for="a in group.announcements" :key="a.id" class="announce-item">
            <div class="announce-top">
              <span class="announce-title">
                <PushpinFilled v-if="a.isPinned" style="color:#f5a623;font-size:12px" />
                {{ a.title }}
              </span>
              <span class="announce-actions" v-if="isAdmin">
                <a-button type="link" size="small" @click="openEditAnnounce(a)"><EditOutlined /></a-button>
                <a-button type="link" size="small"
                  @click="group.toggleAnnouncementPin(a.id, !a.isPinned, group.currentGroup!.id)">
                  {{ a.isPinned ? '取消置顶' : '置顶' }}
                </a-button>
                <a-popconfirm title="确认删除？" @confirm="group.deleteAnnouncement(a.id, group.currentGroup!.id)">
                  <a-button type="link" size="small" danger><DeleteOutlined /></a-button>
                </a-popconfirm>
              </span>
            </div>
            <div class="announce-content">{{ a.content }}</div>
            <div class="announce-time">{{ a.createTime }}</div>
          </div>
          <a-empty v-if="group.announcements.length === 0" description="暂无公告" />
        </a-tab-pane>

        <!-- 入群申请(管理员可见) -->
        <a-tab-pane v-if="isAdmin" key="requests">
          <template #tab>
            申请<span v-if="group.requests.length > 0" style="color:#ff4d4f;margin-left:4px">{{ group.requests.length }}</span>
          </template>
          <div v-for="r in group.requests" :key="r.id" class="request-item">
            <span>用户{{ r.fromUserId }} 申请入群</span>
            <span style="font-size:12px;color:#999">{{ r.verifyMessage }}</span>
            <a-popconfirm title="确认同意入群申请？" @confirm="group.handleRequest(r.id, true, group.currentGroup!.id)"
              okText="确认" cancelText="取消">
              <a-button size="small" type="primary">同意</a-button>
            </a-popconfirm>
            <a-popconfirm title="确认拒绝？" @confirm="group.handleRequest(r.id, false, group.currentGroup!.id)"
              okText="确认" cancelText="取消">
              <a-button size="small">拒绝</a-button>
            </a-popconfirm>
          </div>
          <a-empty v-if="group.requests.length === 0" description="暂无申请" />
        </a-tab-pane>

      </a-tabs>
    </div>

    <a-empty v-else description="选择一个群查看详情" class="detail-empty" />

    <!-- 创建群弹窗 -->
    <a-modal v-model:open="showCreate" title="创建群聊" @ok="createGroup"
      okText="确定" cancelText="取消">
      <a-input v-model:value="newGroupName" placeholder="群名称" />
    </a-modal>

    <!-- 群设置弹窗 -->
    <a-modal v-model:open="showSettings" title="群设置" @ok="saveSettings"
      okText="保存" cancelText="取消">
      <a-form layout="vertical">
        <a-form-item label="群头像">
          <AvatarCropper v-model="settingsForm.avatar" />
        </a-form-item>
        <a-form-item label="群名称">
          <a-input v-model:value="settingsForm.groupName" />
        </a-form-item>
        <a-form-item label="群简介">
          <a-textarea v-model:value="settingsForm.description" :rows="3" />
        </a-form-item>
        <a-form-item label="加群方式">
          <a-select v-model:value="settingsForm.joinType">
            <a-select-option :value="0">自由加入</a-select-option>
            <a-select-option :value="1">需验证</a-select-option>
            <a-select-option :value="2">禁止加入</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="最大成员数">
          <a-input-number v-model:value="settingsForm.maxMembers" :min="2" :max="2000" />
        </a-form-item>
        <a-form-item label="全员禁言">
          <a-switch :checked="settingsForm.isMutedAll === 1"
            @update:checked="settingsForm.isMutedAll = $event ? 1 : 0" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 公告弹窗 -->
    <a-modal v-model:open="showAnnounce" :title="editAnnounceId ? '编辑公告' : '发布公告'" @ok="saveAnnounce"
      okText="保存" cancelText="取消">
      <a-input v-model:value="announceTitle" placeholder="标题" style="margin-bottom:12px" />
      <a-textarea v-model:value="announceContent" placeholder="内容" :rows="5" />
    </a-modal>
  </div>
</template>

<style scoped>
.group-view { display:flex; flex:1; overflow:hidden; }
.group-left { width:300px; background:#fff; border-right:1px solid #f0f0f0; display:flex; flex-direction:column; flex-shrink:0; }
.group-header { padding:8px 12px; display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap; gap:4px; }
.group-list { flex:1; overflow-y:auto; padding:8px; }
.group-item { display:flex; align-items:center; gap:10px; padding:10px; cursor:pointer; border-radius:6px; }
.group-item:hover { background:#f5f5f5; }
.group-item.active { background:#e6f4ff; }
.group-info { flex:1; }
.group-name { font-size:14px; font-weight:500; }
.group-meta { font-size:12px; color:#999; }
.invitation-list { padding:8px; }
.join-section { padding:12px 0; }
.search-results { margin-top:8px; }
.search-result { display:flex; align-items:center; gap:10px; padding:8px; background:#f9f9f9; border-radius:6px; margin-bottom:6px; }
.search-info { flex:1; }

.group-detail { flex:1; padding:20px; overflow-y:auto; background:#f5f5f5; }
.detail-empty { flex:1; display:flex; align-items:center; justify-content:center; background:#f5f5f5; }
.detail-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:16px; }
.detail-header h3 { margin:0; }
.detail-actions { display:flex; gap:8px; }

.member-list { max-height:400px; overflow-y:auto; }
.member-item { display:flex; align-items:center; gap:8px; padding:8px 0; border-bottom:1px solid #f0f0f0; }
.member-name { flex:1; }

.invite-row { padding:12px 0; }
.invite-search { position:relative; }
.invite-dropdown {
  position:absolute; top:100%; left:0; right:0; z-index:10;
  background:#fff; border:1px solid #e8e8e8; border-radius:6px;
  box-shadow:0 4px 12px rgba(0,0,0,0.1); max-height:200px; overflow-y:auto;
}
.invite-item {
  display:flex; align-items:center; gap:8px; padding:8px 12px;
  cursor:pointer;
}
.invite-item:hover { background:#f5f5f5; }
.invite-item-info { display:flex; flex-direction:column; }

.announce-item { padding:12px; background:#fff; border-radius:6px; margin-bottom:8px; }
.announce-top { display:flex; justify-content:space-between; align-items:center; margin-bottom:6px; }
.announce-title { font-weight:600; }
.announce-actions { display:flex; gap:4px; }
.announce-content { font-size:13px; color:#666; white-space:pre-wrap; }
.announce-time { font-size:11px; color:#999; margin-top:4px; }

.request-item { display:flex; align-items:center; gap:8px; padding:8px 0; border-bottom:1px solid #f0f0f0; }
.request-item > span:first-child { flex:1; }
</style>
