import axios from 'axios'
import { ElMessage } from 'element-plus'

export const TOKEN_KEY = 'lab_token'
export const USER_KEY = 'lab_user'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    if (response.config.responseType === 'blob') {
      return response.data
    }
    const payload = response.data
    if (payload && payload.code === 0) {
      return payload.data
    }
    const message = payload?.message || '请求失败'
    ElMessage.error(message)
    return Promise.reject(new Error(message))
  },
  async (error) => {
    const response = error.response
    const config = error.config
    let message = '网络异常，请确认后端服务已启动'

    if (response) {
      if (config?.responseType === 'blob' && response.data instanceof Blob) {
        try {
          const text = await response.data.text()
          message = JSON.parse(text).message || message
        } catch {
          message = '文件请求失败'
        }
      } else {
        message = response.data?.message || `请求失败（${response.status}）`
      }

      if (response.status === 401 && window.location.pathname !== '/login') {
        localStorage.removeItem(TOKEN_KEY)
        localStorage.removeItem(USER_KEY)
        ElMessage.warning(message || '登录已过期，请重新登录')
        window.location.href = '/login?expired=1'
        return Promise.reject(error)
      }
    }

    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request
