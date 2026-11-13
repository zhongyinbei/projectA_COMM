<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '../../stores/auth'
import {
  acceptRepairOrder,
  completeRepairOrder,
  getRepairOrders,
  rejectRepairOrder
} from '../../api/repairOrder'
import OrderStatusTag from '../../components/OrderStatusTag.vue'

const auth = useAuthStore()
const activeTab = ref('pending')
const pendingOrders = ref([])
const myOrders = ref([])
const loading = ref(false)
const submitting = ref(false)
const completeDialog = ref(false)
const completeForm = reactive({ id: null, orderNo: '', repairNote: '' })

const activeRows = computed(() => (activeTab.value === 'pending' ? pendingOrders.value : myOrders.value))
const activeCount = computed(() => pendingOrders.value.length)

function formatDateTime(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 16)
}

async function loadAll() {
  loading.value = true
  try {
    const [pending, all] = await Promise.all([
      getRepairOrders({ page: 1, size: 100, status: 'PENDING' }),
      getRepairOrders({ page: 1, size: 100 })
    ])
    pendingOrders.value = pending.records || []
    myOrders.value = (all.records || []).filter(
      (item) => item.assigneeId === auth.user?.id && item.status !== 'PENDING'
    )
  } finally {
    loading.value = false
  }
}

async function acceptOrder(row) {
  try {
    await ElMessageBox.confirm(
      `确定接下工单 ${row.orderNo} 吗？接单后设备将标记为维修中。`,
      '确认接单',
      { confirmButtonText: '接单', cancelButtonText: '再看看', type: 'info' }
    )
  } catch {
    return
  }
  try {
    await acceptRepairOrder(row.id)
    ElMessage.success('接单成功，请尽快处理设备问题')
    await loadAll()
  } catch {
    /* 错误提示由请求拦截器处理 */
  }
}

async function rejectOrder(row) {
  try {
    await ElMessageBox.confirm(
      `确定拒绝工单 ${row.orderNo} 吗？拒绝后工单将进入终态。`,
      '拒绝工单',
      { confirmButtonText: '确认拒绝', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await rejectRepairOrder(row.id)
    ElMessage.success('工单已拒绝')
    await loadAll()
  } catch {
    /* 错误提示由请求拦截器处理 */
  }
}

function openComplete(row) {
  completeForm.id = row.id
  completeForm.orderNo = row.orderNo
  completeForm.repairNote = ''
  completeDialog.value = true
}

async function submitComplete() {
  if (!completeForm.repairNote.trim()) {
    ElMessage.warning('请填写维修说明')
    return
  }
  submitting.value = true
  try {
    await completeRepairOrder(completeForm.id, { repairNote: completeForm.repairNote.trim() })
    ElMessage.success('工单已完成，设备状态已恢复')
    completeDialog.value = false
    await loadAll()
  } catch {
    /* 错误提示由请求拦截器处理 */
  } finally {
    submitting.value = false
  }
}

onMounted(loadAll)
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2>维修工单</h2>
        <p>先接单再处理，完成后设备会自动恢复为正常状态。</p>
      </div>
      <div class="head-actions">
        <el-button text @click="loadAll">刷新列表</el-button>
      </div>
    </div>

    <section class="panel">
      <div class="workbench">
        <div class="workbench-copy">
          <span class="wb-kicker">MAINTENANCE QUEUE</span>
          <h3>今天有 <em>{{ activeCount }}</em> 张待接单工单</h3>
          <p>接单后工单进入“我的工单”，完成后填写维修说明即可闭环。</p>
        </div>
        <div class="wb-stats">
          <div><span>待接单</span><b>{{ pendingOrders.length }}</b></div>
          <div><span>我的进行中</span><b>{{ myOrders.filter((item) => item.status === 'ACCEPTED').length }}</b></div>
          <div><span>我的已完成</span><b>{{ myOrders.filter((item) => item.status === 'COMPLETED').length }}</b></div>
        </div>
      </div>

      <div class="tab-row">
        <button type="button" :class="['tab-btn', { active: activeTab === 'pending' }]" @click="activeTab = 'pending'">
          待接单 <span class="tab-count">{{ pendingOrders.length }}</span>
        </button>
        <button type="button" :class="['tab-btn', { active: activeTab === 'mine' }]" @click="activeTab = 'mine'">
          我的工单 <span class="tab-count">{{ myOrders.length }}</span>
        </button>
        <span class="tab-hint">{{ activeTab === 'pending' ? '按提交时间倒序展示' : '仅显示我接单的工单' }}</span>
      </div>

      <el-table v-loading="loading" :data="activeRows" style="width: 100%">
        <el-table-column label="工单" width="190">
          <template #default="{ row }">
            <div class="table-title mono">{{ row.orderNo }}</div>
            <div class="table-sub">{{ formatDateTime(row.createdAt) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="设备" min-width="155">
          <template #default="{ row }">
            <div class="table-title">{{ row.equipmentName || '—' }}</div>
            <div class="table-sub">{{ row.equipmentAssetNo || '—' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="故障描述" min-width="220">
          <template #default="{ row }">
            <el-tooltip :content="row.description" placement="top" :show-after="300">
              <div class="desc-clamp">{{ row.description }}</div>
            </el-tooltip>
            <el-image
              v-if="row.imageUrl"
              :src="row.imageUrl"
              :preview-src-list="[row.imageUrl]"
              preview-teleported
              fit="cover"
              class="order-thumb"
            />
          </template>
        </el-table-column>
        <el-table-column prop="reporterName" label="报修人" width="110">
          <template #default="{ row }">{{ row.reporterName || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><OrderStatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="172" fixed="right">
          <template #default="{ row }">
            <div v-if="activeTab === 'pending'" class="action-cell">
              <el-button type="primary" size="small" @click="acceptOrder(row)">接单</el-button>
              <el-button plain size="small" @click="rejectOrder(row)">拒绝</el-button>
            </div>
            <div v-else-if="row.status === 'ACCEPTED'" class="action-cell">
              <el-button type="primary" size="small" @click="openComplete(row)">完成维修</el-button>
            </div>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-copy">
            {{ activeTab === 'pending' ? '当前没有待接单工单，稍后再来看看。' : '还没有接单记录，去“待接单”看看。' }}
          </div>
        </template>
      </el-table>
    </section>

    <el-dialog v-model="completeDialog" title="完成维修" width="480px">
      <div class="dialog-tip">
        工单 <span class="mono">{{ completeForm.orderNo }}</span> 完成后，设备状态将恢复为正常。
      </div>
      <el-form label-position="top">
        <el-form-item label="维修说明">
          <el-input
            v-model="completeForm.repairNote"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            resize="none"
            placeholder="例如：更换 HDMI 线，重新插拔显卡后恢复正常显示。"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitComplete">确认完成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.workbench {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 26px;
  padding: 25px 26px;
  border-bottom: 1px solid var(--line);
  background:
    radial-gradient(circle at 94% 10%, rgba(45, 212, 191, 0.13), transparent 32%),
    linear-gradient(120deg, #f7fbfa, #fff);
}

.wb-kicker {
  color: #0f766e;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 1.8px;
}

.workbench h3 {
  margin: 8px 0 6px;
  font-size: 22px;
  letter-spacing: -0.6px;
}

.workbench h3 em {
  color: #0f766e;
  font-style: normal;
}

.workbench p {
  margin: 0;
  color: #7d8e9d;
  font-size: 12px;
}

.wb-stats {
  display: flex;
  gap: 26px;
  flex-shrink: 0;
}

.wb-stats div {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 76px;
  padding-left: 16px;
  border-left: 1px solid #dce8e4;
}

.wb-stats span {
  color: #8a9baa;
  font-size: 10.5px;
}

.wb-stats b {
  color: #17384a;
  font-size: 23px;
  line-height: 1;
}

.tab-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 18px 0;
}

.tab-btn {
  padding: 8px 13px 11px;
  border: none;
  border-bottom: 2px solid transparent;
  color: #758696;
  background: transparent;
  font-size: 13px;
  cursor: pointer;
}

.tab-btn.active {
  color: #0f766e;
  border-bottom-color: #0f766e;
  font-weight: 600;
}

.tab-count {
  display: inline-grid;
  place-items: center;
  min-width: 18px;
  height: 18px;
  margin-left: 4px;
  padding: 0 5px;
  border-radius: 999px;
  color: #567080;
  background: #eef3f5;
  font-size: 10px;
}

.tab-btn.active .tab-count {
  color: #0f766e;
  background: #e6f4f1;
}

.tab-hint {
  margin-left: auto;
  color: #9aa8b4;
  font-size: 10.5px;
}

.desc-clamp {
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.6;
}

.order-thumb {
  width: 42px;
  height: 42px;
  margin-top: 6px;
  border-radius: 6px;
  background: #f1f5f7;
}

.action-cell {
  display: flex;
  gap: 6px;
}

.dialog-tip {
  margin-bottom: 15px;
  padding: 10px 12px;
  border-radius: 8px;
  color: #68808f;
  background: #f5f9fa;
  font-size: 12px;
  line-height: 1.6;
}

@media (max-width: 900px) {
  .workbench {
    align-items: flex-start;
    flex-direction: column;
  }

  .wb-stats {
    width: 100%;
    gap: 0;
  }

  .wb-stats div {
    flex: 1;
  }
}

@media (max-width: 600px) {
  .tab-hint {
    display: none;
  }

  .wb-stats {
    flex-wrap: wrap;
    gap: 12px;
  }

  .wb-stats div {
    min-width: 40%;
    flex: 0 0 40%;
  }
}
</style>
