<script setup lang="ts">
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/auth'

const router = useRouter()
const auth = useAuthStore()

const activeTab = ref('login')
const form = reactive({ username: '', password: '', nickname: '' })
const loading = ref(false)

async function handleSubmit() {
  if (!form.username || !form.password) {
    message.warning('请填写完整信息')
    return
  }
  loading.value = true
  try {
    if (activeTab.value === 'login') {
      const res = await auth.login(form.username, form.password)
      if (res.code === 200) {
        message.success('登录成功')
        router.push('/')
      }
    } else {
      if (form.password.length < 6) {
        message.warning('密码至少6位')
        loading.value = false
        return
      }
      const res = await authApi.register({
        username: form.username,
        password: form.password,
        nickname: form.nickname || form.username,
      })
      if (res.data.code === 200) {
        message.success('注册成功，请登录')
        activeTab.value = 'login'
      }
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <h1>OnlineChat</h1>
      <a-tabs v-model:activeKey="activeTab" centered>
        <a-tab-pane key="login" tab="登录" />
        <a-tab-pane key="register" tab="注册" />
      </a-tabs>

      <a-form :model="form" @finish="handleSubmit" size="large">
        <a-form-item>
          <a-input v-model:value="form.username" placeholder="用户名" />
        </a-form-item>
        <a-form-item v-if="activeTab === 'register'">
          <a-input v-model:value="form.nickname" placeholder="昵称（选填）" />
        </a-form-item>
        <a-form-item>
          <a-input-password v-model:value="form.password" placeholder="密码" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit" :loading="loading" block size="large">
            {{ activeTab === 'login' ? '登录' : '注册' }}
          </a-button>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #07c160 0%, #1aad19 100%);
}
.login-card {
  width: 400px;
  padding: 32px 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 8px 40px rgba(0,0,0,0.12);
}
.login-card h1 {
  text-align: center;
  margin-bottom: 8px;
  color: #07c160;
  font-size: 28px;
}
</style>
