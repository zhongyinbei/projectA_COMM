<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { getConsumableTopStats, getDashboardSummary, getEquipmentStatusStats, getInspectionRateStats } from '../../api/dashboard'
import { getRepairOrders } from '../../api/repairOrder'

const loading = ref(false)
const summary = ref({
  equipmentTotal: 0,
  pendingOrders: 0,
  abnormalInspections: 0,
  alertConsumables: []
})
const statusData = ref([])
const rateData = ref([])
const topData = ref([])
const orders = ref([])
const updatedAt = ref('')

const equipmentChartRef = ref()
const inspectionChartRef = ref()
const consumableChartRef = ref()
const orderChartRef = ref()
const charts = []

const statusLabel = {
  NORMAL: '正常',
  REPAIRING: '维修中',
  SCRAPPED: '已报废'
}
const orderLabel = {
  PENDING: '待接单',
  ACCEPTED: '维修中',
  REJECTED: '已拒绝',
  COMPLETED: '已完成'
}

const alertList = computed(() => summary.value.alertConsumables || [])
const equipmentTotal = computed(
  () => summary.value.equipmentTotal || statusData.value.reduce((total, item) => total + Number(item.count || 0), 0)
)

const orderStatusData = computed(() => {
  const counts = { PENDING: 0, ACCEPTED: 0, REJECTED: 0, COMPLETED: 0 }
  orders.value.forEach((item) => {
    if (counts[item.status] !== undefined) counts[item.status] += 1
  })
  return Object.keys(counts).map((key) => ({ name: orderLabel[key], value: counts[key] }))
})

function initChart(element, option) {
  if (!element) return
  const instance = echarts.getInstanceByDom(element) || echarts.init(element)
  instance.setOption(option, true)
  if (!charts.includes(instance)) charts.push(instance)
}

function renderCharts() {
  initChart(equipmentChartRef.value, {
    color: ['#0f766e', '#e6a23c', '#94a3b8'],
    tooltip: { trigger: 'item', formatter: '{b}<br/>{c} 台（{d}%）' },
    legend: {
      bottom: 2,
      icon: 'circle',
      itemWidth: 7,
      itemHeight: 7,
      textStyle: { color: '#7b8c9b', fontSize: 10 }
    },
    graphic: [
      {
        type: 'text',
        left: 'center',
        top: '36%',
        style: { text: String(equipmentTotal.value), fill: '#17384a', fontSize: 30, fontWeight: 700, textAlign: 'center' }
      },
      {
        type: 'text',
        left: 'center',
        top: '48%',
        style: { text: '设备总数', fill: '#93a2af', fontSize: 10, textAlign: 'center' }
      }
    ],
    series: [
      {
        type: 'pie',
        radius: ['53%', '72%'],
        center: ['50%', '43%'],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: '#fff', borderWidth: 3, borderRadius: 5 },
        label: { show: false },
        data: statusData.value.map((item) => ({
          name: statusLabel[item.status] || item.status,
          value: Number(item.count)
        }))
      }
    ]
  })

  initChart(inspectionChartRef.value, {
    color: ['#0f766e'],
    tooltip: { trigger: 'axis', axisPointer: { type: 'line' } },
    grid: { left: 36, right: 18, top: 25, bottom: 34 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: rateData.value.map((item) => String(item.date).slice(5)),
      axisLine: { lineStyle: { color: '#e4ebf0' } },
      axisTick: { show: false },
      axisLabel: { color: '#8a9aa7', fontSize: 10 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#eef3f6' } },
      axisLabel: { color: '#8a9aa7', fontSize: 10 }
    },
    series: [
      {
        name: '巡检次数',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 2.5 },
        itemStyle: { color: '#0f766e', borderColor: '#fff', borderWidth: 2 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(15, 118, 110, 0.24)' },
            { offset: 1, color: 'rgba(15, 118, 110, 0.01)' }
          ])
        },
        data: rateData.value.map((item) => Number(item.count))
      }
    ]
  })

  initChart(consumableChartRef.value, {
    color: ['#e0a13b'],
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 42, right: 18, top: 24, bottom: 40 },
    xAxis: {
      type: 'category',
      data: topData.value.map((item) => item.name),
      axisLine: { lineStyle: { color: '#e4ebf0' } },
      axisTick: { show: false },
      axisLabel: { color: '#8a9aa7', fontSize: 10, interval: 0, width: 72, overflow: 'truncate' }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#eef3f6' } },
      axisLabel: { color: '#8a9aa7', fontSize: 10 }
    },
    series: [
      {
        name: '出库数量',
        type: 'bar',
        barWidth: 26,
        itemStyle: {
          borderRadius: [5, 5, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#f2c56f' },
            { offset: 1, color: '#d89a2b' }
          ])
        },
        data: topData.value.map((item) => Number(item.quantity))
      }
    ]
  })

  initChart(orderChartRef.value, {
    color: ['#e6a23c', '#0f766e', '#d14343', '#67c23a'],
    tooltip: { trigger: 'item', formatter: '{b}<br/>{c} 张（{d}%）' },
    legend: {
      bottom: 2,
      icon: 'circle',
      itemWidth: 7,
      itemHeight: 7,
      textStyle: { color: '#7b8c9b', fontSize: 10 }
    },
    series: [
      {
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['50%', '44%'],
        roseType: 'radius',
        itemStyle: { borderColor: '#fff', borderWidth: 3, borderRadius: 5 },
        label: { color: '#6c8090', fontSize: 10, formatter: '{b}\n{c}' },
        labelLine: { length: 8, length2: 8, lineStyle: { color: '#cbd7df' } },
        data: orderStatusData.value
      }
    ]
  })
}

async function loadDashboard() {
  loading.value = true
  try {
    const [summaryData, status, rate, top, orderPage] = await Promise.all([
      getDashboardSummary(),
      getEquipmentStatusStats(),
      getInspectionRateStats(),
      getConsumableTopStats(),
      getRepairOrders({ page: 1, size: 1000 })
    ])
    summary.value = summaryData
    statusData.value = status || []
    rateData.value = rate || []
    topData.value = top || []
    orders.value = orderPage.records || []
    updatedAt.value = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    await nextTick()
    renderCharts()
  } finally {
    loading.value = false
  }
}

function resizeCharts() {
  charts.forEach((chart) => chart.resize())
}

onMounted(() => {
  window.addEventListener('resize', resizeCharts)
  loadDashboard()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  charts.forEach((chart) => chart.dispose())
})
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2>数据看板</h2>
        <p>设备、巡检与耗材数据总览，实时掌握实训室运行状态。</p>
      </div>
      <div class="head-actions">
        <span v-if="updatedAt" class="updated-at">更新于 {{ updatedAt }}</span>
        <el-button type="primary" plain :loading="loading" @click="loadDashboard">刷新数据</el-button>
      </div>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-label">设备总数</div>
        <div class="stat-value">{{ summary.equipmentTotal }}</div>
        <div class="stat-foot">在册实训设备资产</div>
      </div>
      <div class="stat-card warm">
        <div class="stat-label">待接单工单</div>
        <div class="stat-value">{{ summary.pendingOrders }}</div>
        <div class="stat-foot">等待维修员响应</div>
      </div>
      <div class="stat-card danger">
        <div class="stat-label">异常巡检</div>
        <div class="stat-value">{{ summary.abnormalInspections }}</div>
        <div class="stat-foot">累计标记为异常的记录</div>
      </div>
      <div class="stat-card" :class="{ danger: alertList.length > 0 }">
        <div class="stat-label">库存预警</div>
        <div class="stat-value">{{ alertList.length }}</div>
        <div class="stat-foot">低于阈值的耗材种类</div>
      </div>
    </div>

    <el-alert
      v-if="alertList.length > 0"
      type="error"
      :closable="false"
      show-icon
      class="stock-alert"
      title="耗材库存预警"
    >
      <div class="alert-row">
        <span v-for="item in alertList" :key="item.id" class="alert-pill">
          {{ item.name }} · 剩余 {{ item.stock }} {{ item.unit }} / 阈值 {{ item.threshold }}
        </span>
        <router-link to="/admin/consumable" class="alert-link">去补货 →</router-link>
      </div>
    </el-alert>

    <div class="chart-grid" v-loading="loading">
      <section class="panel chart-panel">
        <div class="chart-head">
          <div>
            <h3>设备状态分布</h3>
            <p>正常 / 维修中 / 已报废设备占比</p>
          </div>
          <span class="chart-tag">台账</span>
        </div>
        <div ref="equipmentChartRef" class="chart-box"></div>
      </section>

      <section class="panel chart-panel">
        <div class="chart-head">
          <div>
            <h3>近 7 天巡检趋势</h3>
            <p>按日统计的巡检记录数量</p>
          </div>
          <span class="chart-tag">巡检</span>
        </div>
        <div ref="inspectionChartRef" class="chart-box"></div>
      </section>

      <section class="panel chart-panel">
        <div class="chart-head">
          <div>
            <h3>耗材出库 TOP5</h3>
            <p>累计出库数量最高的耗材</p>
          </div>
          <span class="chart-tag">耗材</span>
        </div>
        <div ref="consumableChartRef" class="chart-box"></div>
      </section>

      <section class="panel chart-panel">
        <div class="chart-head">
          <div>
            <h3>工单状态分布</h3>
            <p>全部报修工单的状态构成</p>
          </div>
          <span class="chart-tag">工单</span>
        </div>
        <div ref="orderChartRef" class="chart-box"></div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.updated-at {
  color: #96a5b1;
  font-size: 10.5px;
}

.stock-alert {
  margin-bottom: 16px;
  border: 1px solid #f3cbc7;
  border-radius: 12px;
  background: #fff7f6;
}

.alert-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 5px;
}

.alert-pill {
  padding: 3px 9px;
  border-radius: 999px;
  color: #a5342c;
  background: #feeceb;
  font-size: 10.5px;
}

.alert-link {
  color: #b42318;
  font-size: 10.5px;
  font-weight: 600;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.chart-panel {
  padding: 19px 20px 14px;
}

.chart-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 4px;
}

.chart-head h3 {
  margin: 0;
  font-size: 15px;
  letter-spacing: -0.2px;
}

.chart-head p {
  margin: 5px 0 0;
  color: #94a3b0;
  font-size: 10.5px;
}

.chart-tag {
  padding: 3px 8px;
  border: 1px solid #e1ecef;
  border-radius: 999px;
  color: #8b9ba8;
  font-size: 9.5px;
}

.chart-box {
  width: 100%;
  height: 300px;
}

@media (max-width: 1000px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .chart-box {
    height: 270px;
  }
}
</style>
