<script setup lang="ts">
import { message } from 'ant-design-vue'
import { useContactStore } from '@/stores/contact'
import { useChatStore } from '@/stores/chat'
import { userApi } from '@/api/user'
import { contactApi } from '@/api/contact'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import {
  UserAddOutlined, SearchOutlined, DeleteOutlined,
  StarOutlined, StarFilled, CheckOutlined, CloseOutlined,
  BlockOutlined, EditOutlined,
} from '@ant-design/icons-vue'

const contact = useContactStore()
const chat = useChatStore()
const auth = useAuthStore()
const router = useRouter()

const activeTab = ref('friends')
const searchUser = ref('')
const searchResult = ref<any>(null)
const showAddFriend = ref(false)
const verifyMessage = ref('')
const editingRemark = ref({ id: 0, remark: '' })

onMounted(async () => {
  await contact.loadAll()
  preloadUserInfo()
})

// 切换子 tab 时重新加载
watch(activeTab, () => {
  contact.loadAll().then(() => preloadUserInfo())
})

function preloadUserInfo() {
  for (const c of contact.contacts) {
    contact.getUserInfo(c.contactUserId)
  }
  for (const b of contact.blocklist) {
    contact.getUserInfo(b.blockedUserId)
  }
}

// 获取好友显示名称
function getDisplayName(contactUserId: number, remark?: string) {
  if (remark) return remark
  const user = contact.userCache.get(contactUserId)
  return user?.nickname || user?.username || `用户${contactUserId}`
}

// 搜索用户
async function handleSearch() {
  if (!searchUser.value.trim()) return
  try {
    const { data } = await userApi.search(searchUser.value.trim())
    if (data.code === 200) searchResult.value = data.data
    else searchResult.value = null
  } catch (e) { searchResult.value = null }
}

// 加好友
async function addFriend() {
  if (searchResult.value) {
    await contact.sendRequest(searchResult.value.id, verifyMessage.value, 'search')
    showAddFriend.value = false
    verifyMessage.value = ''
    message.success('已发送好友申请')
  }
}

// 发起聊天 — 查找已有会话或创建新的
function startChat(contactUser: any) {
  const targetId = contactUser.contactUserId
  const name = getDisplayName(targetId, contactUser.remark)
  // 先查已有会话
  let conv: any = chat.conversations.find((c: any) => c.type === 0 && c.targetId === targetId)
  if (!conv) {
    conv = { id: 0, type: 0, targetId, targetName: name, unreadCount: 0, isPinned: 0, isMuted: 0 }
  } else {
    conv = { ...conv, targetName: name }
  }
  chat.openConversation(conv as any)
  router.push('/')
}

// 备注编辑
function handleDeleteContact(contactUserId: number) {
  if (confirm('确认删除该好友？')) contact.deleteContact(contactUserId)
}

async function saveRemark() {
  if (editingRemark.value.id) {
    await contact.updateRemark(editingRemark.value.id, editingRemark.value.remark)
    editingRemark.value = { id: 0, remark: '' }
    message.success('备注已更新')
  }
}

// 获取好友信息
const contactsWithName = computed(() => {
  return contact.contacts.map((c) => ({
    ...c,
    displayName: c.remark || `用户${c.contactUserId}`,
  }))
})

// 待处理申请
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

      <!-- 好友列表 -->
      <div v-if="activeTab === 'friends'" class="contact-list">
        <div v-for="c in contact.contacts" :key="c.id" class="contact-item" @click="startChat(c)">
          <a-avatar :size="40">{{ getDisplayName(c.contactUserId, c.remark).charAt(0) }}</a-avatar>
          <div class="contact-info">
            <div class="contact-name">
              {{ getDisplayName(c.contactUserId, c.remark) }}
              <StarFilled v-if="c.isStarred" style="color:#f5a623;font-size:12px" />
            </div>
          </div>
          <a-dropdown :trigger="['click']">
            <a-button type="text" size="small"><EditOutlined /></a-button>
            <template #overlay>
              <a-menu>
                <a-menu-item @click="editingRemark = { id: c.contactUserId, remark: c.remark || '' }">
                  修改备注
                </a-menu-item>
                <a-menu-item danger @click="handleDeleteContact(c.contactUserId)">
                  <DeleteOutlined /> 删除好友
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
        <a-empty v-if="contactsWithName.length === 0" description="暂无好友" />
      </div>

      <!-- 好友申请 -->
      <div v-else-if="activeTab === 'requests'" class="contact-list">
        <div class="search-box">
          <a-input-search
            v-model:value="searchUser"
            placeholder="搜索用户名添加好友"
            @search="handleSearch"
          />
        </div>
        <!-- 搜索结果 -->
        <div v-if="searchResult" class="search-result">
          <a-avatar :size="40">{{ searchResult.nickname?.charAt(0) }}</a-avatar>
          <div class="search-info">
            <div>{{ searchResult.nickname }}</div>
            <div style="font-size:12px;color:#999">{{ searchResult.username }}</div>
          </div>
          <a-button type="primary" size="small" @click="showAddFriend = true">
            <UserAddOutlined /> 添加
          </a-button>
        </div>

        <!-- 申请列表 -->
        <div v-for="req in contact.requests" :key="req.id" class="request-item">
          <a-avatar :size="36">?</a-avatar>
          <div class="request-info">
            <div>用户{{ req.fromUserId }} 请求添加好友</div>
            <div style="font-size:12px;color:#999">{{ req.verifyMessage }}</div>
          </div>
          <div v-if="req.status === 0" class="request-actions">
            <a-button type="primary" size="small" @click="contact.handleRequest(req.id, true)">
              <CheckOutlined />
            </a-button>
            <a-button size="small" @click="contact.handleRequest(req.id, false)">
              <CloseOutlined />
            </a-button>
          </div>
          <a-tag v-else :color="req.status === 1 ? 'green' : 'red'">
            {{ req.status === 1 ? '已同意' : '已拒绝' }}
          </a-tag>
        </div>
        <a-empty v-if="!searchResult && contact.requests.length === 0" description="暂无好友申请" />
      </div>

      <!-- 黑名单 -->
      <div v-else class="contact-list">
        <div v-for="b in contact.blocklist" :key="b.id" class="contact-item">
          <a-avatar :size="36">🚫</a-avatar>
          <div class="contact-info">
            <div>用户{{ b.blockedUserId }}</div>
            <div style="font-size:11px;color:#999">{{ b.reason }}</div>
          </div>
          <a-button size="small" @click="contact.unblockUser(b.blockedUserId)">移出黑名单</a-button>
        </div>
        <a-empty v-if="contact.blocklist.length === 0" description="黑名单为空" />
      </div>
    </div>

    <!-- 右侧占位 -->
    <div class="contact-right">
      <a-empty description="选择好友开始聊天" v-if="activeTab === 'friends'" />
    </div>

    <!-- 添加好友弹窗 -->
    <a-modal v-model:open="showAddFriend" title="添加好友" @ok="addFriend">
      <a-form>
        <a-form-item label="验证消息">
          <a-textarea v-model:value="verifyMessage" placeholder="我是..." :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 备注编辑弹窗 -->
    <a-modal :open="editingRemark.id !== 0" title="修改备注" @ok="saveRemark" @cancel="editingRemark = { id: 0, remark: '' }">
      <a-input v-model:value="editingRemark.remark" placeholder="输入备注名" />
    </a-modal>
  </div>
</template>

<style scoped>
.contact-view { display:flex; flex:1; overflow:hidden; }
.contact-left { width:300px; background:#fff; border-right:1px solid #f0f0f0; display:flex; flex-direction:column; flex-shrink:0; }
.contact-header { padding:0 12px; }
.contact-list { flex:1; overflow-y:auto; padding:8px; }
.contact-item { display:flex; align-items:center; gap:10px; padding:10px 8px; cursor:pointer; border-radius:6px; }
.contact-item:hover { background:#f5f5f5; }
.contact-info { flex:1; min-width:0; }
.contact-name { font-size:14px; }
.search-box { padding:8px 0; }
.search-result { display:flex; align-items:center; gap:10px; padding:10px; background:#f9f9f9; border-radius:8px; margin:8px 0; }
.search-info { flex:1; }
.request-item { display:flex; align-items:center; gap:10px; padding:10px 0; border-bottom:1px solid #f0f0f0; }
.request-info { flex:1; }
.request-actions { display:flex; gap:4px; }
.contact-right { flex:1; display:flex; align-items:center; justify-content:center; background:#f5f5f5; }
</style>
