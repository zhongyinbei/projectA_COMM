<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createInspectionRecord,
  getInspectionPoints,
  getInspectionRecords,
  syncInspectionRecords
} from '../../api/inspection'

const STORAGE_KEY = 'pending_inspections'

const isOnline = ref(navigator.onLine)
const pending = ref(readPending())
const points = ref([])
const records = ref([])
const recordsLoading = ref(false)
const submitting = ref(false)
const syncing = ref(false)
const form = ref({
  pointCode: '',
  result: 'NORMAL',
  remark: ''
})

const currentPoint = computed(() => points.value.find((item) => item.pointCode === form.value.pointCode))
const pendingCount = computed(() => pending.value.length)

function readPending() {
  try {
    const value = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]')
    return Array.isArray(value) ? value : []
  } catch {
    return []
  }
}

function savePending() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(pending.value))
}

function formatDateTime(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 16)
}

function localIsoTime(date = new Date()) {
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(
    date.getMinutes()
  )}:${pad(date.getSeconds())}`
}

async function loadPoints() {
  const data = await getInspectionPoints()
  points.value = data || []
}

async function loadRecords() {
  recordsLoading.value = true
  try {
    const data = await getInspectionRecords({ page: 1, size: 10 })
    records.value = data.records || []
  } finally {
    recordsLoading.value = false
  }
}

function selectPoint(code) {
  form.value.pointCode = code
}

async function submitRecord() {
  const pointCode = form.value.pointCode.trim()
  if (!pointCode) {
    ElMessage.warning('请输入巡检点编码')
    return
  }
  if (!['NORMAL', 'ABNORMAL'].includes(form.value.result)) {
    ElMessage.warning('请选择巡检结果')
    return
  }

  const payload = {
    pointCode,
    result: form.value.result,
    remark: form.value.remark.trim() || null
  }

  if (!isOnline.value) {
    pending.value.push({ ...payload, inspectedAt: localIsoTime() })
    savePending()
    ElMessage.warning('当前处于离线状态，巡检记录已保存到本地')
    form.value.remark = ''
    return
  }

  submitting.value = true
  try {
    await createInspectionRecord(payload)
    ElMessage.success('巡检记录已提交')
    form.value.remark = ''
    await loadRecords()
  } catch {
    /* 错误提示由请求拦截器处理 */
  } finally {
    submitting.value = false
  }
}

async function syncPending(manual = false) {
  if (syncing.value || pending.value.length === 0) return
  if (!isOnline.value) {
    if (manual) ElMessage.warning('当前网络离线，恢复网络后会自动同步')
    return
  }
  syncing.value = true
  const snapshot = [...pending.value]
  try {
    const result = await syncInspectionRecords(snapshot)
    if (result.failed === 0) {
      pending.value = []
      localStorage.removeItem(STORAGE_KEY)
      ElMessage.success(`离线巡检已同步 ${result.success} 条`)
    } else {
      ElMessage.warning(`同步完成：成功 ${result.success} 条，失败 ${result.failed} 条，本地记录已保留，请检查后重试`)
    }
    await loadRecords()
  } catch {
    /* 错误提示由请求拦截器处理 */
  } finally {
    syncing.value = false
  }
}

function clearPending() {
  pending.value = []
  localStorage.removeItem(STORAGE_KEY)
  ElMessage.info('本地待同步记录已清空')
}

function handleOnline() {
  isOnline.value = true
  ElMessage.success('网络已恢复，正在自动同步离线巡检')
  syncPending()
}

function handleOffline() {
  isOnline.value = false
  ElMessage.warning('网络已断开，巡检记录将保存在本地')
}

onMounted(async () => {
  window.addEventListener('online', handleOnline)
  window.addEventListener('offline', handleOffline)
  await Promise.all([loadPoints(), loadRecords()])
  if (isOnline.value && pending.value.length > 0) {
    syncPending()
  }
})

onUnmounted(() => {
  window.removeEventListener('online', handleOnline)
  window.removeEventListener('offline', handleOffline)
})
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2>巡检作业</h2>
        <p>输入巡检点编码模拟扫码，在线直接提交；断网时自动保存在本地。</p>
      </div>
      <div class="head-actions">
        <el-button text @click="loadRecords">刷新记录</el-button>
      </div>
    </div>

    <section :class="['connect-bar', { offline: !isOnline }]">
      <div class="connect-status">
        <span class="status-orb"></span>
        <div>
          <b>{{ isOnline ? '在线巡检' : '离线巡检中' }}</b>
          <small>{{ isOnline ? '提交后立即写入服务器' : '记录暂存在本机，网络恢复后自动同步' }}</small>
        </div>
      </div>
      <div class="connect-divider"></div>
      <div class="pending-stat">
        <span>离线待同步</span>
        <b>{{ pendingCount }}</b>
        <small>条</small>
      </div>
      <div class="connect-actions">
        <el-button
          type="primary"
          plain
          :loading="syncing"
          :disabled="pendingCount === 0 || !isOnline"
          @click="syncPending(true)"
        >
          立即同步
        </el-button>
      </div>
    </section>

    <div class="split-grid">
      <section class="panel panel-pad">
        <div class="section-title">
          <h3>提交巡检记录</h3>
          <span class="hint">模拟扫码 · 无需摄像头</span>
        </div>

        <el-form label-position="top">
          <el-form-item label="巡检点编码">
            <el-input
              v-model="form.pointCode"
              size="large"
              placeholder="例如 P-001"
              clearable
              @keyup.enter="submitRecord"
            >
              <template #append>
                <span class="scan-hint">SCAN</span>
              </template>
            </el-input>
          </el-form-item>

          <div class="quick-points">
            <span class="quick-label">快速选择</span>
            <button
              v-for="point in points.slice(0, 6)"
              :key="point.id"
              type="button"
              :class="['point-chip', { active: form.pointCode === point.pointCode }]"
              @click="selectPoint(point.pointCode)"
            >
              {{ point.pointCode }}
            </button>
            <span v-if="points.length === 0" class="muted quick-empty">暂无巡检点</span>
          </div>

          <div v-if="currentPoint" class="point-summary">
            <span class="point-pin">◎</span>
            <div>
              <b>{{ currentPoint.pointCode }}</b>
              <small>{{ currentPoint.location }}</small>
            </div>
          </div>

          <el-form-item label="巡检结果">
            <el-radio-group v-model="form.result" class="result-group">
              <el-radio-button label="NORMAL">正常</el-radio-button>
              <el-radio-button label="ABNORMAL">异常</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="备注（选填）">
            <el-input
              v-model="form.remark"
              type="textarea"
              :rows="3"
              maxlength="255"
              show-word-limit
              resize="none"
              placeholder="记录现场情况，例如：网线松动、设备温度偏高…"
            />
          </el-form-item>

          <div class="submit-row">
            <span class="submit-hint">
              {{ isOnline ? '提交后立即生效' : '离线保存，联网后自动同步' }}
            </span>
            <el-button type="primary" :loading="submitting" @click="submitRecord">
              {{ isOnline ? '提交巡检记录' : '保存到本地' }}
            </el-button>
          </div>
        </el-form>
      </section>

      <aside class="side-column">
        <section class="panel panel-pad pending-panel">
          <div class="section-title">
            <h3>离线待同步</h3>
            <span class="pending-badge">{{ pendingCount }}</span>
          </div>
          <div v-if="pendingCount === 0" class="pending-empty">
            <span class="empty-orb">✓</span>
            <p>所有巡检记录均已同步</p>
            <small>断网时提交的记录会出现在这里</small>
          </div>
          <div v-else class="pending-list">
            <div v-for="(item, index) in pending.slice(-4).reverse()" :key="`${item.inspectedAt}-${index}`" class="pending-item">
              <span :class="['result-dot', item.result === 'ABNORMAL' ? 'abnormal' : 'normal']"></span>
              <div>
                <b>{{ item.pointCode }}</b>
                <small>{{ formatDateTime(item.inspectedAt) }}</small>
              </div>
              <span class="pending-result">{{ item.result === 'ABNORMAL' ? '异常' : '正常' }}</span>
            </div>
            <button type="button" class="clear-pending" @click="clearPending">清空本地记录</button>
          </div>
        </section>

        <section class="panel panel-pad guide-panel">
          <div class="guide-title">离线巡检说明</div>
          <p>将浏览器 Network 设为 Offline 后提交，记录会写入 localStorage 的 <code>pending_inspections</code>。</p>
          <p>恢复网络后页面会自动批量同步，后端将记录标记为离线来源。</p>
        </section>
      </aside>
    </div>

    <section class="panel records-panel">
      <div class="toolbar">
        <div class="grow">
          <div class="section-title" style="margin: 0">
            <h3>最近巡检记录</h3>
            <span class="hint">最新 10 条</span>
          </div>
        </div>
        <el-button text @click="loadRecords">刷新</el-button>
      </div>
      <el-table v-loading="recordsLoading" :data="records" style="width: 100%">
        <el-table-column prop="pointCode" label="巡检点" width="115">
          <template #default="{ row }">
            <span class="mono point-code">{{ row.pointCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="位置" min-width="175" />
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag :type="row.result === 'ABNORMAL' ? 'danger' : 'success'" effect="light" round>
              {{ row.result === 'ABNORMAL' ? '异常' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180">
          <template #default="{ row }">
            <span :class="row.remark ? '' : 'muted'">{{ row.remark || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="inspectorName" label="巡检人" width="110" />
        <el-table-column label="巡检时间" width="155">
          <template #default="{ row }">{{ formatDateTime(row.inspectedAt) }}</template>
        </el-table-column>
        <el-table-column label="来源" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.syncedFromOffline === 1" type="warning" effect="plain" size="small">离线同步</el-tag>
            <span v-else class="muted">在线</span>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-copy">还没有巡检记录，提交第一条吧。</div>
        </template>
      </el-table>
    </section>
  </div>
</template>

<style scoped>
.connect-bar {
  display: flex;
  align-items: center;
  gap: 22px;
  margin-bottom: 16px;
  padding: 15px 20px;
  border: 1px solid #cfe7df;
  border-radius: 14px;
  background: linear-gradient(120deg, #f2fbf8, #fff);
}

.connect-bar.offline {
  border-color: #f1ddbd;
  background: linear-gradient(120deg, #fffaf0, #fff);
}

.connect-status {
  display: flex;
  align-items: center;
  gap: 11px;
}

.status-orb {
  width: 10px;
  height: 10px;
  flex-shrink: 0;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 5px rgba(34, 197, 94, 0.12);
}

.offline .status-orb {
  background: #f59e0b;
  box-shadow: 0 0 0 5px rgba(245, 158, 11, 0.12);
}

.connect-status b {
  display: block;
  color: #204757;
  font-size: 13px;
}

.connect-status small {
  display: block;
  margin-top: 2px;
  color: #8697a4;
  font-size: 10.5px;
}

.connect-divider {
  width: 1px;
  height: 30px;
  background: #dce8e4;
}

.offline .connect-divider {
  background: #efe3cd;
}

.pending-stat {
  display: flex;
  align-items: baseline;
  gap: 5px;
  color: #7d8f9d;
  font-size: 11px;
}

.pending-stat b {
  color: #0f766e;
  font-size: 25px;
  line-height: 1;
}

.offline .pending-stat b {
  color: #b45309;
}

.pending-stat small {
  font-size: 10px;
}

.connect-actions {
  margin-left: auto;
}

.scan-hint {
  color: #0f766e;
  font-family: "Cascadia Mono", Consolas, monospace;
  font-size: 9px;
  letter-spacing: 1px;
}

.quick-points {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin: -8px 0 18px;
}

.quick-label {
  margin-right: 2px;
  color: #93a2af;
  font-size: 10.5px;
}

.point-chip {
  padding: 4px 9px;
  border: 1px solid #dce7ec;
  border-radius: 999px;
  color: #627685;
  background: #fff;
  font-family: "Cascadia Mono", Consolas, monospace;
  font-size: 10px;
  cursor: pointer;
}

.point-chip:hover,
.point-chip.active {
  color: #0f766e;
  border-color: #9bcac2;
  background: #eefaf6;
}

.quick-empty {
  font-size: 10px;
}

.point-summary {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: -4px 0 20px;
  padding: 11px 13px;
  border: 1px solid #e1ecef;
  border-radius: 10px;
  background: #fafcfd;
}

.point-pin {
  color: #0f766e;
  font-size: 18px;
}

.point-summary div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.point-summary b {
  color: #2d4658;
  font-size: 12px;
}

.point-summary small {
  color: #8a9aa7;
  font-size: 10.5px;
}

.result-group :deep(.el-radio-button__inner) {
  min-width: 82px;
}

.submit-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 4px;
}

.submit-hint {
  color: #99a7b3;
  font-size: 10.5px;
}

.side-column {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.pending-badge {
  display: inline-grid;
  place-items: center;
  min-width: 22px;
  height: 22px;
  border-radius: 999px;
  color: #0f766e;
  background: #e6f4f1;
  font-size: 11px;
  font-weight: 600;
}

.pending-empty {
  padding: 18px 8px 10px;
  text-align: center;
}

.empty-orb {
  display: inline-grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  color: #0f766e;
  background: #e9f6f2;
  font-size: 14px;
}

.pending-empty p {
  margin: 9px 0 3px;
  color: #5e7484;
  font-size: 12px;
}

.pending-empty small {
  color: #9aa8b4;
  font-size: 10px;
}

.pending-list {
  display: grid;
  gap: 8px;
}

.pending-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 9px;
  border: 1px solid #eef3f5;
  border-radius: 9px;
  background: #fbfcfd;
}

.result-dot {
  width: 6px;
  height: 6px;
  flex-shrink: 0;
  border-radius: 50%;
}

.result-dot.normal {
  background: #22c55e;
}

.result-dot.abnormal {
  background: #ef4444;
}

.pending-item div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.pending-item b {
  color: #3b5364;
  font-family: "Cascadia Mono", Consolas, monospace;
  font-size: 10.5px;
}

.pending-item small {
  color: #97a5b1;
  font-size: 9px;
}

.pending-result {
  margin-left: auto;
  color: #7d8e9c;
  font-size: 9.5px;
}

.clear-pending {
  margin-top: 4px;
  padding: 6px;
  border: 1px dashed #e1e9ee;
  border-radius: 7px;
  color: #9aa8b4;
  background: transparent;
  font-size: 10px;
  cursor: pointer;
}

.clear-pending:hover {
  color: #c2410c;
  border-color: #f1c7b4;
}

.guide-panel {
  background: linear-gradient(160deg, #f8fafc, #fff);
}

.guide-title {
  color: #40586a;
  font-size: 12px;
  font-weight: 700;
}

.guide-panel p {
  margin: 9px 0 0;
  color: #8393a1;
  font-size: 10.5px;
  line-height: 1.75;
}

.guide-panel code {
  padding: 1px 4px;
  border-radius: 3px;
  color: #0f766e;
  background: #eaf6f3;
  font-size: 9px;
}

.records-panel {
  margin-top: 16px;
}

.point-code {
  color: #0f766e;
}

@media (max-width: 820px) {
  .connect-bar {
    align-items: flex-start;
    flex-wrap: wrap;
    gap: 13px;
  }

  .connect-divider {
    display: none;
  }

  .connect-actions {
    margin-left: 0;
  }

  .pending-stat {
    margin-left: auto;
  }
}
</style>
