<script setup>
import { computed } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const nav = [
  { path: '/admin/dashboard', label: '数据看板', mark: '览' },
  { path: '/admin/equipment', label: '设备台账', mark: '设' },
  { path: '/admin/consumable', label: '耗材库存', mark: '耗' },
  { path: '/admin/inspection-point', label: '巡检点管理', mark: '巡' }
]

const title = computed(() => route.meta.title || '管理后台')
const initial = computed(() => (auth.user?.realName || '管').slice(0, 1))
const today = new Intl.DateTimeFormat('zh-CN', {
  month: 'long',
  day: 'numeric',
  weekday: 'long'
}).format(new Date())

async function handleLogout() {
  try {
    await ElMessageBox.confirm('退出后将需要重新登录，确定退出吗？', '退出登录', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning'
    })
    auth.logout()
    router.replace('/login')
  } catch {
    /* 用户取消 */
  }
}
</script>

<template>
  <div class="shell">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-mark">LC</span>
        <div>
          <div class="brand-name">LabCare</div>
          <div class="brand-sub">管理端 · ADMIN</div>
        </div>
      </div>

      <span class="role-badge">实训室运营中心</span>

      <nav class="side-nav">
        <router-link
          v-for="item in nav"
          :key="item.path"
          :to="item.path"
          :class="{ active: route.path === item.path }"
        >
          <span class="nav-dot">{{ item.mark }}</span>
          {{ item.label }}
        </router-link>
      </nav>

      <div class="sidebar-foot">
        <div class="user-chip">
          <span class="avatar">{{ initial }}</span>
          <div>
            <div class="user-name">{{ auth.user?.realName }}</div>
            <div class="user-meta">{{ auth.user?.username }}</div>
          </div>
        </div>
        <button class="logout-btn" type="button" @click="handleLogout">退出登录</button>
      </div>
    </aside>

    <div class="main-area">
      <header class="topbar">
        <div>
          <div class="topbar-kicker">Operations Console</div>
          <h1>运营管理中心</h1>
        </div>
        <div class="top-actions">
          <span class="muted date-text">{{ today }}</span>
        </div>
      </header>
      <main class="content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.date-text {
  font-size: 12px;
}
</style>
