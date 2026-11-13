import { defineStore } from 'pinia'
import { login as loginApi } from '../api/auth'
import { TOKEN_KEY, USER_KEY } from '../api/request'

export function homePath(role) {
  if (role === 'REPORTER') return '/reporter/home'
  if (role === 'REPAIRER') return '/repairer/orders'
  if (role === 'ADMIN') return '/admin/dashboard'
  return '/login'
}

export function decodeRole(token) {
  try {
    const payload = token.split('.')[1]
    if (!payload) return null
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
    const parsed = JSON.parse(decodeURIComponent(escape(window.atob(normalized))))
    return parsed.role || null
  } catch {
    return null
  }
}

function readUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: readUser()
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token && state.user),
    roleHome: (state) => homePath(state.user?.role)
  },
  actions: {
    async login(credentials) {
      const data = await loginApi(credentials)
      this.token = data.token
      this.user = data.user
      localStorage.setItem(TOKEN_KEY, data.token)
      localStorage.setItem(USER_KEY, JSON.stringify(data.user))
      return data.user
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }
})
