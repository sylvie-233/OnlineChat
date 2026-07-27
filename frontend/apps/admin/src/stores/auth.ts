import { defineStore } from 'pinia'
import http from '@/api'

export const useAdminAuthStore = defineStore('adminAuth', () => {
  const token = ref(localStorage.getItem('admin_token') || '')
  const nickname = ref('')

  async function login(username: string, password: string) {
    const { data } = await http.post('/api/auth/login', { username, password })
    if (data?.code === 200 && data?.data) {
      token.value = data.data.token
      nickname.value = data.data.nickname
      localStorage.setItem('admin_token', token.value)
    }
    return data
  }

  function logout() {
    token.value = ''
    localStorage.clear()
  }

  const isLoggedIn = computed(() => !!token.value)

  return { token, nickname, login, logout, isLoggedIn }
})
