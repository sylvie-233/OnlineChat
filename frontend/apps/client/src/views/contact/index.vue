<script setup lang="ts">
import { message, Modal } from 'ant-design-vue'
import { useContactStore } from '@/stores/contact'
import { useChatStore } from '@/stores/chat'
import { userApi } from '@/api/user'
import { contactApi } from '@/api/contact'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import type { Contact, ContactGroup } from '@/types'
import {
  UserAddOutlined, SearchOutlined, DeleteOutlined,
  StarOutlined, StarFilled, CheckOutlined, CloseOutlined,
  BlockOutlined, EditOutlined, PlusOutlined, DownOutlined,
  RightOutlined, FolderOutlined, SwapOutlined,
} from '@ant-design/icons-vue'

const contact = useContactStore()
const chat = useChatStore()
const auth = useAuthStore()
const router = useRouter()

const activeTab = ref('friends')
const searchUser = ref('')
const searchResults = ref<any[]>([])
const showAddFriend = ref(false)
const selectedUser = ref<any>(null)
const verifyMessage = ref('')
const editingRemark = ref({ id: 0, remark: '' })
const showMoveGroup = ref(false)
const moveTarget = ref<Contact | null>(null)
const moveGroupId = ref<number>(0)

// 分组管理
const showNewGroup = ref(false)
const newGroupName = ref('')
const editingGroup = ref({ id: 0, name: '' })
// 折叠的分组
const collapsedGroups = ref<Set<number>>(new Set())

onMounted(async () => {
  await contact.loadAll()
  preloadUserInfo()
})

watch(activeTab, () => {
  contact.loadAll().then(() => preloadUserInfo())
})

function preloadUserInfo() {
  for (const c of contact.contacts) contact.getUserInfo(c.contactUserId)
  for (const r of contact.requests) contact.getUserInfo(r.fromUserId)
  for (const b of contact.blocklist) contact.getUserInfo(b.blockedUserId)
}

// 获取好友显示名称
function getDisplayName(contactUserId: number, remark?: string) {
  if (remark) return remark
  const user = contact.userCache.get(contactUserId)
  return user?.nickname || user?.username || `用户${contactUserId}`
}

// 获取头像
function getAvatar(contactUserId: number) {
  return contact.userCache.get(contactUserId)?.avatar || ''
}

// ==================== 分组 ====================

// 按分组归类好友
const groupedContacts = computed(() => {
  const map = new Map<number, Contact[]>()
  // 默认分组
  map.set(0, [])
  for (const g of contact.groups) map.set(g.id, [])
  for (const c of contact.contacts) {
    const gid = c.groupId || 0
    if (!map.has(gid)) map.set(gid, [])
    map.get(gid)!.push(c)
  }
  return map
})

function getGroupName(groupId: number) {
  if (groupId === 0) return '默认分组'
  const g = contact.groups.find(g => g.id === groupId)
  return g?.groupName || '未分组'
}

function toggleGroup(groupId: number) {
  if (collapsedGroups.value.has(groupId)) {
    collapsedGroups.value.delete(groupId)
  } else {
    collapsedGroups.value.add(groupId)
  }
  collapsedGroups.value = new Set(collapsedGroups.value)
}

function isCollapsed(groupId: number) {
  return collapsedGroups.value.has(groupId)
}

async function createGroup() {
  if (!newGroupName.value.trim()) return
  await contactApi.createGroup(newGroupName.value.trim())
  newGroupName.value = ''
  showNewGroup.value = false
  message.success('分组已创建')
  await contact.loadAll()
}

async function renameGroup() {
  if (!editingGroup.value.name.trim()) return
  await contactApi.renameGroup(editingGroup.value.id, editingGroup.value.name.trim())
  editingGroup.value = { id: 0, name: '' }
  message.success('分组已重命名')
  await contact.loadAll()
}

async function deleteGroup(groupId: number) {
  await contactApi.deleteGroup(groupId)
  message.success('分组已删除')
  await contact.loadAll()
}

// ==================== 好友操作 ====================

function startChat(contactUser: any) {
  const targetId = contactUser.contactUserId
  const name = getDisplayName(targetId, contactUser.remark)
  let conv: any = chat.conversations.find((c: any) => c.type === 0 && c.targetId === targetId)
  if (!conv) {
    conv = { id: 0, type: 0, targetId, targetName: name, unreadCount: 0, isPinned: 0, isMuted: 0 }
  } else {
    conv = { ...conv, targetName: name }
  }
  chat.openConversation(conv as any)
  router.push('/')
}

function handleDeleteContact(contactUserId: number) {
  Modal.confirm({
    title: '删除好友', content: '确认删除该好友？',
    okText: '确认删除', cancelText: '取消', okType: 'danger',
    async onOk() { await contact.deleteContact(contactUserId) },
  })
}

async function toggleStar(c: Contact) {
  await contactApi.toggleStar(c.contactUserId, !c.isStarred)
  await contact.loadAll()
  preloadUserInfo()
}

async function handleBlock(c: Contact) {
  Modal.confirm({
    title: '拉黑用户', content: '拉黑后双方无法互发消息，确认拉黑？',
    okText: '确认拉黑', cancelText: '取消', okType: 'danger',
    async onOk() {
      await contact.blockUser(c.contactUserId, '手动拉黑')
      message.success('已拉黑')
    },
  })
}

function openMoveGroup(c: Contact) {
  moveTarget.value = c
  moveGroupId.value = c.groupId || 0
  showMoveGroup.value = true
}

async function doMoveGroup() {
  if (!moveTarget.value) return
  await contactApi.moveToGroup(moveTarget.value.contactUserId, moveGroupId.value)
  showMoveGroup.value = false
  moveTarget.value = null
  message.success('已移动')
  await contact.loadAll()
}

async function saveRemark() {
  if (editingRemark.value.id) {
    await contact.updateRemark(editingRemark.value.id, editingRemark.value.remark)
    editingRemark.value = { id: 0, remark: '' }
    message.success('备注已更新')
  }
}

// ==================== 搜索 ====================

async function handleSearch() {
  if (!searchUser.value.trim()) return
  try {
    const { data } = await userApi.search(searchUser.value.trim())
    if (data.code === 200) searchResults.value = data.data || []
    else searchResults.value = []
  } catch (e) { searchResults.value = [] }
}

function isContact(userId: number) {
  return contact.contacts.some(c => c.contactUserId === userId)
}

function openAddFriend(user: any) {
  selectedUser.value = user
  showAddFriend.value = true
}

async function addFriend() {
  if (selectedUser.value) {
    await contact.sendRequest(selectedUser.value.id, verifyMessage.value, 'search')
    showAddFriend.value = false
    verifyMessage.value = ''
    selectedUser.value = null
    message.success('已发送好友申请')
  }
}

// ==================== 好友申请 ====================

const pendingRequests = computed(() =>
  contact.requests.filter((r) => r.status === 0),
)
</script>

<template>
  <div class="contact-view">
    <!-- 左侧列表 -->
    <div class="contact-left">
      <div class="contact-header">
        <a-tabs v-model:activeKey="activeTab" size="small">
          <a-tab-pane key="friends" tab="好友" />
          <a-tab-pane key="requests" :tab="`新朋友(${pendingRequests.length})`" />
          <a-tab-pane key="blocked" tab="黑名单" />
        </a-tabs>
      </div>

      <!-- ========== 好友列表 ========== -->
      <div v-if="activeTab === 'friends'" class="contact-list">
        <!-- 分组工具栏 -->
        <div class="group-toolbar">
          <a-button type="dashed" size="small" block @click="showNewGroup = true">
            <PlusOutlined /> 新建分组
          </a-button>
        </div>

        <!-- 分组区 -->
        <div v-for="groupId in [...groupedContacts.keys()].sort((a, b) => a - b)" :key="groupId" class="group-section">
          <div class="group-header" @click="toggleGroup(groupId)">
            <span class="group-arrow">
              <RightOutlined v-if="isCollapsed(groupId)" style="font-size:10px" />
              <DownOutlined v-else style="font-size:10px" />
            </span>
            <FolderOutlined />
            <span class="group-name">{{ getGroupName(groupId) }}</span>
            <span class="group-count">{{ groupedContacts.get(groupId)?.length || 0 }}</span>
            <span v-if="groupId !== 0" class="group-actions" @click.stop>
              <a-button
                type="text" size="small"
                @click="editingGroup = { id: groupId, name: getGroupName(groupId) }"
              >
                <EditOutlined style="font-size:11px" />
              </a-button>
              <a-popconfirm title="删除分组？好友移入默认分组" @confirm="deleteGroup(groupId)">
                <a-button type="text" size="small" danger><DeleteOutlined style="font-size:11px" /></a-button>
              </a-popconfirm>
            </span>
          </div>

          <div v-show="!isCollapsed(groupId)" class="group-contacts">
            <div
              v-for="c in groupedContacts.get(groupId)"
              :key="c.id"
              class="contact-item"
              @click="startChat(c)"
            >
              <a-avatar :size="36" :src="getAvatar(c.contactUserId)">
                {{ getDisplayName(c.contactUserId, c.remark).charAt(0) }}
              </a-avatar>
              <div class="contact-info">
                <div class="contact-name">
                  {{ getDisplayName(c.contactUserId, c.remark) }}
                  <StarFilled v-if="c.isStarred" style="color:#f5a623;font-size:12px" />
                </div>
              </div>
              <a-dropdown :trigger="['click']" @click.stop>
                <a-button type="text" size="small"><EditOutlined /></a-button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item @click.stop="editingRemark = { id: c.contactUserId, remark: c.remark || '' }">
                      <EditOutlined /> 修改备注
                    </a-menu-item>
                    <a-menu-item @click.stop="toggleStar(c)">
                      <template v-if="c.isStarred"><StarFilled style="color:#f5a623" /> 取消星标</template>
                      <template v-else><StarOutlined /> 设为星标</template>
                    </a-menu-item>
                    <a-menu-item @click.stop="openMoveGroup(c)">
                      <SwapOutlined /> 移动到分组
                    </a-menu-item>
                    <a-menu-divider />
                    <a-menu-item danger @click.stop="handleBlock(c)">
                      <BlockOutlined /> 拉黑
                    </a-menu-item>
                    <a-menu-item danger @click.stop="handleDeleteContact(c.contactUserId)">
                      <DeleteOutlined /> 删除好友
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </div>
            <div v-if="(groupedContacts.get(groupId)?.length || 0) === 0" class="group-empty">
              暂无好友
            </div>
          </div>
        </div>
      </div>

      <!-- ========== 好友申请 ========== -->
      <div v-else-if="activeTab === 'requests'" class="contact-list">
        <div class="search-box">
          <a-input-search
            v-model:value="searchUser"
            placeholder="搜索用户名或昵称添加好友"
            @search="handleSearch"
          />
        </div>
        <!-- 搜索结果 -->
        <div v-if="searchResults.length > 0" class="search-results">
          <div v-for="u in searchResults" :key="u.id" class="search-result">
            <a-avatar :size="40">{{ (u.nickname || u.username).charAt(0) }}</a-avatar>
            <div class="search-info">
              <div>{{ u.nickname }}</div>
              <div style="font-size:12px;color:#999">@{{ u.username }}</div>
            </div>
            <a-tag v-if="u.id === auth.userId" color="default">自己</a-tag>
            <a-tag v-else-if="isContact(u.id)" color="green">已是好友</a-tag>
            <a-button v-else type="primary" size="small" @click="openAddFriend(u)">
              <UserAddOutlined /> 添加
            </a-button>
          </div>
        </div>

        <!-- 申请列表 -->
        <div style="margin-top:12px;font-size:13px;color:#666" v-if="contact.requests.length > 0">好友申请</div>
        <div v-for="req in contact.requests" :key="req.id" class="request-item">
          <a-avatar :size="36">?</a-avatar>
          <div class="request-info">
            <div>{{ getDisplayName(req.fromUserId) }} 请求添加好友</div>
            <div style="font-size:12px;color:#999">{{ req.verifyMessage }}</div>
          </div>
          <div v-if="req.status === 0" class="request-actions">
            <a-popconfirm title="确认同意该好友申请？" @confirm="contact.handleRequest(req.id, true)"
              okText="确认" cancelText="取消">
              <a-button type="primary" size="small"><CheckOutlined /></a-button>
            </a-popconfirm>
            <a-popconfirm title="确认拒绝该好友申请？" @confirm="contact.handleRequest(req.id, false)"
              okText="确认" cancelText="取消">
              <a-button size="small"><CloseOutlined /></a-button>
            </a-popconfirm>
          </div>
          <a-tag v-else :color="req.status === 1 ? 'green' : 'red'">
            {{ req.status === 1 ? '已同意' : '已拒绝' }}
          </a-tag>
        </div>
        <a-empty v-if="searchResults.length === 0 && contact.requests.length === 0" description="暂无好友申请" />
      </div>

      <!-- ========== 黑名单 ========== -->
      <div v-else class="contact-list">
        <div v-for="b in contact.blocklist" :key="b.id" class="contact-item">
          <a-avatar :size="36">🚫</a-avatar>
          <div class="contact-info">
            <div>{{ getDisplayName(b.blockedUserId) }}</div>
            <div style="font-size:11px;color:#999">{{ b.reason }}</div>
          </div>
          <a-button size="small" @click="contact.unblockUser(b.blockedUserId)">移出黑名单</a-button>
        </div>
        <a-empty v-if="contact.blocklist.length === 0" description="黑名单为空" />
      </div>
    </div>

    <!-- 右侧占位 -->
    <div class="contact-right">
      <a-empty description="选择好友开始聊天" />
    </div>

    <!-- 添加好友弹窗 -->
    <a-modal v-model:open="showAddFriend" title="添加好友" @ok="addFriend"
      @cancel="selectedUser = null; verifyMessage = ''" okText="确定" cancelText="取消">
      <a-form>
        <a-form-item label="对方">
          {{ selectedUser?.nickname }} (@{{ selectedUser?.username }})
        </a-form-item>
        <a-form-item label="验证消息">
          <a-textarea v-model:value="verifyMessage" placeholder="我是..." :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 备注编辑弹窗 -->
    <a-modal :open="editingRemark.id !== 0" title="修改备注" @ok="saveRemark"
      @cancel="editingRemark = { id: 0, remark: '' }" okText="保存" cancelText="取消">
      <a-input v-model:value="editingRemark.remark" placeholder="输入备注名" />
    </a-modal>

    <!-- 移动分组弹窗 -->
    <a-modal v-model:open="showMoveGroup" title="移动到分组" @ok="doMoveGroup"
      @cancel="showMoveGroup = false; moveTarget = null" okText="确定" cancelText="取消">
      <a-select v-model:value="moveGroupId" style="width:100%">
        <a-select-option :value="0">默认分组</a-select-option>
        <a-select-option v-for="g in contact.groups" :key="g.id" :value="g.id">
          {{ g.groupName }}
        </a-select-option>
      </a-select>
    </a-modal>

    <!-- 重命名分组弹窗 -->
    <a-modal :open="editingGroup.id !== 0" title="重命名分组" @ok="renameGroup"
      @cancel="editingGroup = { id: 0, name: '' }" okText="保存" cancelText="取消">
      <a-input v-model:value="editingGroup.name" placeholder="输入分组名" />
    </a-modal>

    <!-- 新建分组弹窗 -->
    <a-modal v-model:open="showNewGroup" title="新建分组" @ok="createGroup"
      @cancel="showNewGroup = false; newGroupName = ''" okText="确定" cancelText="取消">
      <a-input v-model:value="newGroupName" placeholder="输入分组名称" />
    </a-modal>
  </div>
</template>

<style scoped>
.contact-view { display:flex; flex:1; overflow:hidden; }
.contact-left { width:320px; background:#fff; border-right:1px solid #f0f0f0; display:flex; flex-direction:column; flex-shrink:0; }
.contact-header { padding:0 12px; flex-shrink:0; }
.contact-list { flex:1; overflow-y:auto; padding:8px; }

/* 分组 */
.group-toolbar { padding: 4px 0 8px; }
.group-section { margin-bottom: 2px; }
.group-header {
  display:flex; align-items:center; gap:6px;
  padding:8px 4px; cursor:pointer; user-select:none;
  border-radius:4px;
}
.group-header:hover { background:#f0f0f0; }
.group-arrow { color:#999; width:14px; display:inline-flex; align-items:center; justify-content:center; }
.group-name { font-size:13px; font-weight:500; flex:1; }
.group-count { font-size:11px; color:#999; }
.group-actions { display:flex; }
.group-contacts { padding-left: 8px; }
.group-empty { font-size:12px; color:#ccc; text-align:center; padding:12px; }

/* 联系人 */
.contact-item { display:flex; align-items:center; gap:10px; padding:8px; cursor:pointer; border-radius:6px; }
.contact-item:hover { background:#f5f5f5; }
.contact-info { flex:1; min-width:0; }
.contact-name { font-size:13px; }

/* 搜索 */
.search-box { padding:4px 0 8px; }
.search-results { margin-top: 4px; }
.search-result { display:flex; align-items:center; gap:10px; padding:10px; background:#f9f9f9; border-radius:8px; margin-bottom:6px; }
.search-info { flex:1; }

/* 申请 */
.request-item { display:flex; align-items:center; gap:10px; padding:10px 0; border-bottom:1px solid #f0f0f0; }
.request-info { flex:1; }
.request-actions { display:flex; gap:4px; }

.contact-right { flex:1; display:flex; align-items:center; justify-content:center; background:#f5f5f5; }
</style>
