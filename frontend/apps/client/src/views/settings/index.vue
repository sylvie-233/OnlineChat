<script setup lang="ts">
import { message } from 'ant-design-vue'
import { useAuthStore } from '@/stores/auth'
import { userApi } from '@/api/user'
import type { UserSetting } from '@/types'
import SessionManager from './SessionManager.vue'

const auth = useAuthStore()
const profile = reactive({
  nickname: auth.nickname,
  avatar: auth.avatar,
  phone: '',
  email: '',
  gender: 0,
  bio: '',
  region: '',
})
const setting = reactive<Partial<UserSetting>>({})
const loading = ref(false)

onMounted(async () => {
  try {
    const [uRes, sRes] = await Promise.all([
      userApi.getById(auth.userId),
      userApi.getSettings(),
    ])
    if (uRes.data.code === 200) {
      const u = uRes.data.data
      Object.assign(profile, {
        nickname: u.nickname, avatar: u.avatar,
        phone: u.phone, email: u.email,
        gender: u.gender, bio: u.bio, region: u.region,
      })
    }
    if (sRes.data.code === 200) {
      Object.assign(setting, sRes.data.data)
    }
  } catch (e) { /* ignore */ }
})

async function saveProfile() {
  loading.value = true
  try {
    await userApi.updateProfile(profile)
    auth.nickname = profile.nickname
    auth.avatar = profile.avatar
    localStorage.setItem('nickname', profile.nickname)
    localStorage.setItem('avatar', profile.avatar)
    message.success('保存成功')
  } finally { loading.value = false }
}

async function saveSettings() {
  try {
    await userApi.updateSettings(setting)
    message.success('设置已保存')
  } catch (e) { /* ignore */ }
}
</script>

<template>
  <div class="settings-view">
    <h3>个人设置</h3>

    <a-tabs>
      <a-tab-pane key="profile" tab="个人资料">
        <a-form layout="vertical" style="max-width:500px">
          <a-form-item label="昵称">
            <a-input v-model:value="profile.nickname" />
          </a-form-item>
          <a-form-item label="头像URL">
            <a-input v-model:value="profile.avatar" />
          </a-form-item>
          <a-form-item label="手机号">
            <a-input v-model:value="profile.phone" />
          </a-form-item>
          <a-form-item label="邮箱">
            <a-input v-model:value="profile.email" />
          </a-form-item>
          <a-form-item label="性别">
            <a-radio-group v-model:value="profile.gender">
              <a-radio :value="0">未知</a-radio>
              <a-radio :value="1">男</a-radio>
              <a-radio :value="2">女</a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item label="地区">
            <a-input v-model:value="profile.region" />
          </a-form-item>
          <a-form-item label="个人简介">
            <a-textarea v-model:value="profile.bio" :rows="3" />
          </a-form-item>
          <a-button type="primary" :loading="loading" @click="saveProfile">保存资料</a-button>
        </a-form>
      </a-tab-pane>

      <a-tab-pane key="prefs" tab="偏好设置">
        <a-form layout="vertical" style="max-width:500px">
          <a-form-item label="消息通知">
            <a-switch v-model:checked="(setting.msgNotifyEnabled as any)" :checked-value="1" :un-checked-value="0" />
          </a-form-item>
          <a-form-item label="声音">
            <a-switch v-model:checked="(setting.soundEnabled as any)" :checked-value="1" :un-checked-value="0" />
          </a-form-item>
          <a-form-item label="振动">
            <a-switch v-model:checked="(setting.vibrateEnabled as any)" :checked-value="1" :un-checked-value="0" />
          </a-form-item>
          <a-form-item label="通知显示详情">
            <a-switch v-model:checked="(setting.showDetailEnabled as any)" :checked-value="1" :un-checked-value="0" />
          </a-form-item>
          <a-form-item label="主题">
            <a-select v-model:value="setting.theme" style="width:120px">
              <a-select-option value="light">浅色</a-select-option>
              <a-select-option value="dark">深色</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="字体大小">
            <a-select v-model:value="setting.fontSize" style="width:120px">
              <a-select-option value="small">小</a-select-option>
              <a-select-option value="medium">中</a-select-option>
              <a-select-option value="large">大</a-select-option>
            </a-select>
          </a-form-item>
          <a-button type="primary" @click="saveSettings">保存设置</a-button>
        </a-form>
      </a-tab-pane>

      <a-tab-pane key="sessions" tab="设备管理">
        <SessionManager />
      </a-tab-pane>

      <a-tab-pane key="privacy" tab="隐私">
        <a-form layout="vertical" style="max-width:500px">
          <a-form-item label="好友验证方式">
            <a-radio-group v-model:value="setting.friendVerifyType">
              <a-radio :value="0">允许所有人</a-radio>
              <a-radio :value="1">需要验证</a-radio>
              <a-radio :value="2">拒绝所有人</a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item label="群邀请验证">
            <a-radio-group v-model:value="setting.groupInviteVerify">
              <a-radio :value="0">允许所有人</a-radio>
              <a-radio :value="1">需要验证</a-radio>
              <a-radio :value="2">拒绝</a-radio>
            </a-radio-group>
          </a-form-item>
          <a-button type="primary" @click="saveSettings">保存隐私设置</a-button>
        </a-form>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<style scoped>
.settings-view { flex:1; overflow-y:auto; padding:24px; background:#fff; }
.settings-view h3 { margin-bottom:20px; }
</style>
