<script setup lang="ts">
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { useAdminAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAdminAuthStore()
const form = reactive({ username: '', password: '' })
const loading = ref(false)

async function handleLogin() {
  if (!form.username || !form.password) {
    message.warning('请填写完整信息')
    return
  }
  loading.value = true
  try {
    const res = await auth.login(form.username, form.password)
    if (res.code === 200) {
      message.success('登录成功')
      router.push('/')
    }
  } finally { loading.value = false }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <h1>OnlineChat Admin</h1>
      <a-form :model="form" @finish="handleLogin" size="large">
        <a-form-item>
          <a-input v-model:value="form.username" placeholder="管理员账号" />
        </a-form-item>
        <a-form-item>
          <a-input-password v-model:value="form.password" placeholder="密码" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit" :loading="loading" block>
            登录管理后台
          </a-button>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<style scoped>
.login-page { height:100vh; display:flex; align-items:center; justify-content:center; background:#001529; }
.login-card { width:400px; padding:40px; background:#fff; border-radius:8px; }
.login-card h1 { text-align:center; margin-bottom:24px; color:#001529; }
</style>
