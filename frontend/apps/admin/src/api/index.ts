import axios from 'axios'
import { message } from 'ant-design-vue'

const http = axios.create({
  baseURL: '/',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_token')
  if (token) {
    config.headers.Authorization = token
  }
  return config
})

http.interceptors.response.use(
  (res) => res,
  (err) => {
    message.error(err.response?.data?.message || '网络错误')
    return Promise.reject(err)
  }
)

export default http
