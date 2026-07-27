import axios from 'axios'
import { message } from 'ant-design-vue'

const http = axios.create({
  baseURL: '/',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = token
  return config
})

http.interceptors.response.use(
  (res) => {
    if (res.data?.code !== 200 && res.config.method !== 'options') {
      message.error(res.data?.message || '请求失败')
      return Promise.reject(res.data)
    }
    return res
  },
  (err) => {
    const msg = err.response?.data?.message || '网络错误'
    message.error(msg)
    if (err.response?.status === 401) {
      localStorage.clear()
      window.location.href = '/login'
    }
    return Promise.reject(err)
  },
)

export default http
