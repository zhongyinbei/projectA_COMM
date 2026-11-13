import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore, decodeRole, homePath } from '../stores/auth'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/Login.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/reporter',
    component: () => import('../layouts/ReporterLayout.vue'),
    meta: { role: 'REPORTER' },
    redirect: '/reporter/home',
    children: [
      { path: 'home', component: () => import('../views/reporter/Home.vue'), meta: { title: '工作台' } },
      { path: 'repair-new', component: () => import('../views/reporter/RepairNew.vue'), meta: { title: '发起报修' } },
      { path: 'my-orders', component: () => import('../views/reporter/MyOrders.vue'), meta: { title: '我的工单' } }
    ]
  },
  {
    path: '/repairer',
    component: () => import('../layouts/RepairerLayout.vue'),
    meta: { role: 'REPAIRER' },
    redirect: '/repairer/orders',
    children: [
      { path: 'orders', component: () => import('../views/repairer/OrderList.vue'), meta: { title: '维修工单' } },
      { path: 'inspection', component: () => import('../views/repairer/Inspection.vue'), meta: { title: '巡检作业' } }
    ]
  },
  {
    path: '/admin',
    component: () => import('../layouts/AdminLayout.vue'),
    meta: { role: 'ADMIN' },
    redirect: '/admin/dashboard',
    children: [
      { path: 'dashboard', component: () => import('../views/admin/Dashboard.vue'), meta: { title: '数据看板' } },
      { path: 'equipment', component: () => import('../views/admin/Equipment.vue'), meta: { title: '设备台账' } },
      { path: 'consumable', component: () => import('../views/admin/Consumable.vue'), meta: { title: '耗材库存' } },
      { path: 'inspection-point', component: () => import('../views/admin/InspectionPoint.vue'), meta: { title: '巡检点' } }
    ]
  },
  { path: '/', redirect: '/login' },
  { path: '/:pathMatch(.*)*', redirect: '/login' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  const tokenRole = auth.token ? decodeRole(auth.token) : null

  if (to.path === '/login') {
    if (auth.token && auth.user && tokenRole === auth.user.role) {
      return homePath(auth.user.role)
    }
    return true
  }

  if (!auth.token || !auth.user) {
    auth.logout()
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (!tokenRole || tokenRole !== auth.user.role) {
    auth.logout()
    return { path: '/login', query: { reason: 'invalid' } }
  }

  const requiredRole = to.matched.map((item) => item.meta?.role).find(Boolean)
  if (requiredRole && requiredRole !== auth.user.role) {
    return homePath(auth.user.role)
  }

  return true
})

export default router
