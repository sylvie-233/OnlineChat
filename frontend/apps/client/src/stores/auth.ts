import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api'
import { wsClient } from '@/api/ws'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(Number(localStorage.getItem('userId')) || 0)
  const nickname = ref(localStorage.getItem('nickname') || '')
  const avatar = ref(localStorage.getItem('avatar') || '')

  async function login(username: string, password: string) {
    const { data } = await api.post('/api/auth/login', { username, password })
    if (data.code === 200) {
      token.value = data.data.token
      userId.value = data.data.userId
      nickname.value = data.data.nickname
      avatar.value = data.data.avatar
      localStorage.setItem('token', token.value)
      localStorage.setItem('userId', String(userId.value))
      localStorage.setItem('nickname', nickname.value)
      localStorage.setItem('avatar', avatar.value)
    }
    return data
  }

  async function logout() {
    // 先清客户端状态，保证路由守卫立即生效
    token.value = ''
    userId.value = 0
    nickname.value = ''
    avatar.value = ''
    localStorage.clear()
    wsClient.close()
    // 再通知后端（异步，不阻塞跳转）
    try { await api.post('/api/auth/logout') } catch { /* ignore */ }
  }

  return { token, userId, nickname, avatar, login, logout }
})
