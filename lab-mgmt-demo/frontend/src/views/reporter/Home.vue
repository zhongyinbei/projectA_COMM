<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { getRepairOrders } from '../../api/repairOrder'
import OrderStatusTag from '../../components/OrderStatusTag.vue'

const router = useRouter()
const auth = useAuthStore()
const orders = ref([])
const loading = ref(false)

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 11) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const stats = computed(() => ({
  total: orders.value.length,
  active: orders.value.filter((item) => ['PENDING', 'ACCEPTED'].includes(item.status)).length,
  completed: orders.value.filter((item) => item.status === 'COMPLETED').length
}))

const recent = computed(() => orders.value.slice(0, 5))

const today = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric'
}).format(new Date())

function formatDateTime(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 16)
}

async function loadOrders() {
  loading.value = true
  try {
    const data = await getRepairOrders({ page: 1, size: 1000 })
    orders.value = data.records || []
  } finally {
    loading.value = false
  }
}

onMounted(loadOrders)
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2>报修工作台</h2>
        <p>查看报修进度，快速发起新的设备报修。</p>
      </div>
      <div class="head-actions">
        <el-button type="primary" @click="router.push('/reporter/repair-new')">发起报修</el-button>
      </div>
    </div>

    <section class="welcome panel">
      <div class="welcome-copy">
        <span class="welcome-kicker">REPORTER DESK · {{ today }}</span>
        <h3>{{ greeting }}，{{ auth.user?.realName }}</h3>
        <p>发现设备异常时，请尽量描述故障现象与所在位置，维修员会更快找到问题。</p>
        <div class="welcome-actions">
          <el-button type="primary" @click="router.push('/reporter/repair-new')">提交一张新报修</el-button>
          <el-button text class="ghost-btn" @click="router.push('/reporter/my-orders')">查看全部工单 →</el-button>
        </div>
      </div>
      <div class="welcome-side">
        <div class="side-line"><span>待处理</span><b>{{ stats.active }}</b></div>
        <div class="side-line"><span>已完成</span><b>{{ stats.completed }}</b></div>
        <div class="side-note">报修提交后会进入待接单队列，处理状态实时更新。</div>
      </div>
    </section>

    <div class="stat-grid three">
      <div class="stat-card">
        <div class="stat-label">我的报修总数</div>
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-foot">累计提交的报修工单</div>
      </div>
      <div class="stat-card warm">
        <div class="stat-label">进行中的工单</div>
        <div class="stat-value">{{ stats.active }}</div>
        <div class="stat-foot">待接单与维修中的工单</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">已完成维修</div>
        <div class="stat-value">{{ stats.completed }}</div>
        <div class="stat-foot">维修员已完成并恢复设备</div>
      </div>
    </div>

    <div class="split-grid">
      <section class="panel">
        <div class="toolbar">
          <div class="grow">
            <div class="section-title" style="margin: 0">
              <h3>最近报修</h3>
              <span class="hint">最近 5 条</span>
            </div>
          </div>
          <el-button text @click="loadOrders">刷新</el-button>
        </div>
        <el-table v-loading="loading" :data="recent" style="width: 100%">
          <el-table-column prop="orderNo" label="工单号" width="185">
            <template #default="{ row }">
              <span class="mono">{{ row.orderNo }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="equipmentName" label="设备" min-width="170">
            <template #default="{ row }">
              <div class="table-title">{{ row.equipmentName || '—' }}</div>
              <div class="table-sub">{{ row.equipmentAssetNo || '—' }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="104">
            <template #default="{ row }"><OrderStatusTag :status="row.status" /></template>
          </el-table-column>
          <el-table-column prop="createdAt" label="提交时间" width="150">
            <template #default="{ row }"><span class="muted">{{ formatDateTime(row.createdAt) }}</span></template>
          </el-table-column>
          <template #empty>
            <div class="empty-copy">还没有报修记录，发现设备异常时可以从右上角发起报修。</div>
          </template>
        </el-table>
      </section>

      <aside class="panel panel-pad tips-panel">
        <div class="section-title">
          <h3>报修小贴士</h3>
          <span class="hint">更快处理</span>
        </div>
        <div class="tip-list">
          <div class="tip">
            <span class="tip-index">01</span>
            <div><b>描述具体现象</b><p>例如“开机后指示灯闪烁、显示器无信号”，比“坏了”更容易定位。</p></div>
          </div>
          <div class="tip">
            <span class="tip-index">02</span>
            <div><b>补充现场照片</b><p>可上传设备铭牌、故障画面或接口状态的照片，单张不超过 5MB。</p></div>
          </div>
          <div class="tip">
            <span class="tip-index">03</span>
            <div><b>关注工单状态</b><p>待接单 → 维修中 → 已完成，进展会同步显示在“我的工单”。</p></div>
          </div>
        </div>
        <div class="legend">
          <span><i class="dot pending"></i>待接单</span>
          <span><i class="dot accepted"></i>维修中</span>
          <span><i class="dot done"></i>已完成</span>
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.stat-grid.three {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.welcome {
  position: relative;
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 28px;
  overflow: hidden;
  margin-bottom: 16px;
  padding: 28px 30px;
  color: #effcf9;
  border: none;
  background:
    radial-gradient(circle at 88% 0%, rgba(45, 212, 191, 0.24), transparent 38%),
    linear-gradient(130deg, #0c2533, #113f43 62%, #0f4d49);
}

.welcome::after {
  content: "";
  position: absolute;
  right: 18%;
  bottom: -96px;
  width: 220px;
  height: 220px;
  border: 1px solid rgba(94, 234, 212, 0.16);
  border-radius: 50%;
}

.welcome-copy {
  position: relative;
  z-index: 1;
  max-width: 650px;
}

.welcome-kicker {
  color: #83b8b2;
  font-size: 10px;
  letter-spacing: 1.3px;
}

.welcome h3 {
  margin: 10px 0 8px;
  font-size: 26px;
  letter-spacing: -0.7px;
}

.welcome p {
  max-width: 570px;
  margin: 0;
  color: #9fbdbe;
  font-size: 13px;
  line-height: 1.8;
}

.welcome-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 21px;
}

.ghost-btn {
  color: #b7d7d4;
}

.ghost-btn:hover {
  color: #fff;
}

.welcome-side {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  width: 210px;
  flex-shrink: 0;
  padding: 4px 0 4px 24px;
  border-left: 1px solid rgba(255, 255, 255, 0.12);
}

.side-line {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: 5px 0;
  color: #a7c2c1;
  font-size: 12px;
}

.side-line b {
  color: #f0fdfa;
  font-size: 23px;
  line-height: 1;
}

.side-note {
  margin-top: 11px;
  color: #7e9e9d;
  font-size: 10px;
  line-height: 1.6;
}

.tips-panel {
  align-self: start;
}

.tip-list {
  display: grid;
  gap: 16px;
}

.tip {
  display: flex;
  gap: 11px;
}

.tip-index {
  color: #0f766e;
  font-family: "Cascadia Mono", Consolas, monospace;
  font-size: 10px;
  line-height: 1.8;
}

.tip b {
  color: #31475a;
  font-size: 13px;
}

.tip p {
  margin: 5px 0 0;
  color: #7b8c9d;
  font-size: 11.5px;
  line-height: 1.7;
}

.legend {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 20px;
  padding-top: 15px;
  border-top: 1px solid var(--line);
  color: #8495a4;
  font-size: 10.5px;
}

.legend span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.dot.pending {
  background: #e6a23c;
}

.dot.accepted {
  background: #0f766e;
}

.dot.done {
  background: #67c23a;
}

@media (max-width: 980px) {
  .stat-grid.three {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .welcome-side {
    display: none;
  }
}

@media (max-width: 760px) {
  .welcome {
    padding: 22px;
  }

  .welcome h3 {
    font-size: 22px;
  }

  .welcome-actions {
    align-items: flex-start;
    flex-direction: column;
  }

  .stat-grid.three {
    grid-template-columns: 1fr;
  }
}
</style>
