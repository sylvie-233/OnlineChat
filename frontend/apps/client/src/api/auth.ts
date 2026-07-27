import http from './index'

export const authApi = {
  login(data: { username: string; password: string }) {
    return http.post('/api/auth/login', data)
  },
  register(data: { username: string; password: string; nickname: string }) {
    return http.post('/api/auth/register', data)
  },
  logout() {
    return http.post('/api/auth/logout')
  },
  refresh() {
    return http.post('/api/auth/refresh')
  },
}
