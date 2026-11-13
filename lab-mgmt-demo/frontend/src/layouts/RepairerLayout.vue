<script setup>
import { computed } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const nav = [
  { path: '/repairer/orders', label: '维修工单', mark: '单' },
  { path: '/repairer/inspection', label: '巡检作业', mark: '巡' }
]

const title = computed(() => route.meta.title || '维修工作台')
const initial = computed(() => (auth.user?.realName || '维').slice(0, 1))
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
          <div class="brand-sub">维修员端 · REPAIRER</div>
        </div>
      </div>

      <span class="role-badge">维修保障组</span>

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
          <div class="topbar-kicker">Maintenance Workspace</div>
          <h1>维修保障中心</h1>
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
