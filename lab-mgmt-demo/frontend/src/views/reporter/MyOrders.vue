<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getRepairOrders } from '../../api/repairOrder'
import OrderStatusTag from '../../components/OrderStatusTag.vue'

const router = useRouter()
const orders = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const status = ref('')

function formatDateTime(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 16)
}

async function loadOrders() {
  loading.value = true
  try {
    const data = await getRepairOrders({
      page: page.value,
      size: size.value,
      status: status.value || undefined
    })
    orders.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  loadOrders()
}

function reset() {
  status.value = ''
  page.value = 1
  loadOrders()
}

function handlePage(value) {
  page.value = value
  loadOrders()
}

function handleSize(value) {
  size.value = value
  page.value = 1
  loadOrders()
}

onMounted(loadOrders)
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2>我的工单</h2>
        <p>查看所有报修记录与维修进度，共 {{ total }} 条。</p>
      </div>
      <div class="head-actions">
        <el-button type="primary" @click="router.push('/reporter/repair-new')">发起报修</el-button>
      </div>
    </div>

    <section class="panel">
      <div class="toolbar">
        <el-select v-model="status" placeholder="全部状态" clearable style="width: 150px" @change="search">
          <el-option label="待接单" value="PENDING" />
          <el-option label="维修中" value="ACCEPTED" />
          <el-option label="已拒绝" value="REJECTED" />
          <el-option label="已完成" value="COMPLETED" />
        </el-select>
        <el-button @click="search">筛选</el-button>
        <el-button text @click="reset">重置</el-button>
        <span class="grow"></span>
        <span class="toolbar-tip">状态更新后，点击刷新获取最新进度</span>
        <el-button text @click="loadOrders">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="orders" style="width: 100%">
        <el-table-column label="工单" width="195">
          <template #default="{ row }">
            <div class="table-title mono">{{ row.orderNo }}</div>
            <div class="table-sub">{{ formatDateTime(row.createdAt) }}</div>
          </template>
        </el-table-column>

        <el-table-column label="设备" min-width="175">
          <template #default="{ row }">
            <div class="table-title">{{ row.equipmentName || '—' }}</div>
            <div class="table-sub">{{ row.equipmentAssetNo || '—' }}</div>
          </template>
        </el-table-column>

        <el-table-column label="故障描述" min-width="230">
          <template #default="{ row }">
            <div class="description-cell">
              <el-image
                v-if="row.imageUrl"
                :src="row.imageUrl"
                :preview-src-list="[row.imageUrl]"
                preview-teleported
                fit="cover"
                class="order-thumb"
              />
              <span>{{ row.description }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="105">
          <template #default="{ row }"><OrderStatusTag :status="row.status" /></template>
        </el-table-column>

        <el-table-column label="维修员" width="115">
          <template #default="{ row }">
            <span :class="row.assigneeName ? '' : 'muted'">{{ row.assigneeName || '待分配' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="维修说明" min-width="190">
          <template #default="{ row }">
            <el-tooltip v-if="row.repairNote" :content="row.repairNote" placement="top">
              <span class="repair-note">{{ row.repairNote }}</span>
            </el-tooltip>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>

        <el-table-column label="完成时间" width="150">
          <template #default="{ row }">
            <span class="muted">{{ formatDateTime(row.completedAt) }}</span>
          </template>
        </el-table-column>

        <template #empty>
          <div class="empty-copy">当前筛选条件下暂无工单。</div>
        </template>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="handlePage"
          @size-change="handleSize"
        />
      </div>
    </section>
  </div>
</template>

<style scoped>
.description-cell {
  display: flex;
  align-items: flex-start;
  gap: 9px;
}

.description-cell span {
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.65;
}

.order-thumb {
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  border-radius: 6px;
  background: #f1f5f7;
}

.repair-note {
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  color: #697b8c;
  line-height: 1.6;
  cursor: default;
}

@media (max-width: 760px) {
  .pagination-row {
    justify-content: center;
    overflow-x: auto;
  }
}
</style>
