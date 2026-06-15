import { defineStore } from 'pinia'
import request from '@/utils/request'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: null,
    permissions: []
  }),

  actions: {
    async login(username, password) {
      const res = await request.post('/auth/login', { username, password })
      this.token = res.data.token
      this.permissions = res.data.permissions || []
      localStorage.setItem('token', this.token)
      await this.fetchUserInfo()
      return res
    },

    async fetchUserInfo() {
      try {
        const res = await request.get('/auth/userinfo')
        this.userInfo = res.data
        this.permissions = res.data.permissions || []
      } catch {
        // token invalid
        this.logout()
      }
    },

    hasPermission(perm) {
      return this.permissions.includes(perm)
    },

    logout() {
      this.token = ''
      this.userInfo = null
      this.permissions = []
      localStorage.removeItem('token')
    }
  }
})
