<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createConsumable,
  getConsumables,
  inboundConsumable,
  outboundConsumable
} from '../../api/consumable'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const submitting = ref(false)
const stockSubmitting = ref(false)
const keyword = ref('')
const createDialog = ref(false)
const stockDialog = ref(false)
const formRef = ref()
const form = reactive({
  name: '',
  spec: '',
  unit: '',
  stock: 0,
  threshold: 5
})
const stockForm = reactive({
  id: null,
  name: '',
  stock: 0,
  threshold: 0,
  mode: 'OUT',
  quantity: 1,
  remark: ''
})

const rules = {
  name: [{ required: true, message: '请输入耗材名称', trigger: 'blur' }],
  unit: [{ required: true, message: '请输入计量单位', trigger: 'blur' }]
}

const lowCount = computed(() => rows.value.filter((item) => item.stock < item.threshold).length)

function formatDate(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 16)
}

function rowClassName({ row }) {
  return row.stock < row.threshold ? 'low-stock-row' : ''
}

async function load() {
  loading.value = true
  try {
    const data = await getConsumables({
      page: page.value,
      size: size.value,
      keyword: keyword.value.trim() || undefined
    })
    rows.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function reset() {
  keyword.value = ''
  page.value = 1
  load()
}

function openCreate() {
  Object.assign(form, { name: '', spec: '', unit: '', stock: 0, threshold: 5 })
  createDialog.value = true
  formRef.value?.clearValidate()
}

async function saveCreate() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  if (Number(form.stock) < 0 || Number(form.threshold) < 0) {
    ElMessage.warning('库存和阈值不能为负数')
    return
  }
  submitting.value = true
  try {
    await createConsumable({
      name: form.name.trim(),
      spec: form.spec.trim() || null,
      unit: form.unit.trim(),
      stock: Number(form.stock),
      threshold: Number(form.threshold)
    })
    ElMessage.success('耗材已新增')
    createDialog.value = false
    await load()
  } catch {
    /* 错误提示由请求拦截器处理 */
  } finally {
    submitting.value = false
  }
}

function openStock(row, mode) {
  Object.assign(stockForm, {
    id: row.id,
    name: row.name,
    stock: row.stock,
    threshold: row.threshold,
    mode,
    quantity: 1,
    remark: ''
  })
  stockDialog.value = true
}

async function submitStock() {
  if (!Number.isInteger(Number(stockForm.quantity)) || Number(stockForm.quantity) <= 0) {
    ElMessage.warning('数量必须为大于 0 的整数')
    return
  }
  stockSubmitting.value = true
  try {
    const payload = { quantity: Number(stockForm.quantity), remark: stockForm.remark.trim() || null }
    const data =
      stockForm.mode === 'IN'
        ? await inboundConsumable(stockForm.id, payload)
        : await outboundConsumable(stockForm.id, payload)
    ElMessage.success(`${stockForm.mode === 'IN' ? '入库' : '出库'}成功，当前库存 ${data.stock}`)
    stockDialog.value = false
    await load()
  } catch {
    /* 错误提示由请求拦截器处理 */
  } finally {
    stockSubmitting.value = false
  }
}

function handlePage(value) {
  page.value = value
  load()
}

function handleSize(value) {
  size.value = value
  page.value = 1
  load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2>耗材库存</h2>
        <p>登记耗材入库与领用出库，库存低于阈值时看板会同步告警。</p>
      </div>
      <div class="head-actions">
        <el-button type="primary" @click="openCreate">新增耗材</el-button>
      </div>
    </div>

    <section class="panel">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索耗材名称 / 规格"
          clearable
          style="width: 250px"
          @keyup.enter="search"
        />
        <el-button type="primary" plain @click="search">查询</el-button>
        <el-button text @click="reset">重置</el-button>
        <span class="grow"></span>
        <span v-if="lowCount > 0" class="low-hint">本页 {{ lowCount }} 项低于阈值</span>
        <span class="toolbar-tip">共 {{ total }} 种耗材</span>
        <el-button text @click="load">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" :row-class-name="rowClassName" style="width: 100%">
        <el-table-column prop="name" label="耗材名称" min-width="165">
          <template #default="{ row }">
            <div class="table-title">{{ row.name }}</div>
            <div class="table-sub">{{ row.spec || '无规格' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column label="当前库存" width="110">
          <template #default="{ row }">
            <span :class="['stock-number', { low: row.stock < row.threshold }]">{{ row.stock }}</span>
            <span class="unit-text">{{ row.unit }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="threshold" label="预警阈值" width="105">
          <template #default="{ row }"><span class="muted">{{ row.threshold }} {{ row.unit }}</span></template>
        </el-table-column>
        <el-table-column label="库存状态" width="125">
          <template #default="{ row }">
            <el-tag v-if="row.stock < row.threshold" type="danger" effect="light" round>库存告警</el-tag>
            <el-tag v-else type="success" effect="plain" round>库存充足</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="155">
          <template #default="{ row }"><span class="muted">{{ formatDate(row.createdAt) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openStock(row, 'IN')">入库</el-button>
            <el-button link type="warning" @click="openStock(row, 'OUT')">出库</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-copy">暂无耗材记录，点击“新增耗材”建立库存。</div>
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

    <el-dialog v-model="createDialog" title="新增耗材" width="540px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="耗材名称" prop="name">
          <el-input v-model="form.name" maxlength="100" placeholder="例如 网线水晶头" />
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="form.spec" maxlength="100" placeholder="例如 RJ45 六类" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="计量单位" prop="unit">
            <el-input v-model="form.unit" maxlength="20" placeholder="例如 盒" />
          </el-form-item>
          <el-form-item label="初始库存">
            <el-input-number v-model="form.stock" :min="0" :precision="0" style="width: 100%" />
          </el-form-item>
        </div>
        <el-form-item label="预警阈值">
          <el-input-number v-model="form.threshold" :min="0" :precision="0" style="width: 100%" />
          <div class="field-hint">库存低于该数值时，看板会出现耗材告警。</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="saveCreate">新增耗材</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="stockDialog"
      :title="stockForm.mode === 'IN' ? '耗材入库' : '耗材出库'"
      width="460px"
    >
      <div class="stock-summary">
        <div>
          <span class="muted">耗材</span>
          <b>{{ stockForm.name }}</b>
        </div>
        <div class="stock-now">
          <span>当前库存</span>
          <b>{{ stockForm.stock }}</b>
          <small>阈值 {{ stockForm.threshold }}</small>
        </div>
      </div>
      <el-form label-position="top">
        <el-form-item :label="stockForm.mode === 'IN' ? '入库数量' : '出库数量'">
          <el-input-number v-model="stockForm.quantity" :min="1" :precision="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="stockForm.remark"
            type="textarea"
            :rows="2"
            maxlength="255"
            show-word-limit
            resize="none"
            :placeholder="stockForm.mode === 'IN' ? '例如：学期初采购入库' : '例如：A302 机房领用'"
          />
        </el-form-item>
      </el-form>
      <div v-if="stockForm.mode === 'OUT' && stockForm.quantity > stockForm.stock" class="overdraw-tip">
        出库数量超过当前库存，提交后将返回“库存不足”。
      </div>
      <template #footer>
        <el-button @click="stockDialog = false">取消</el-button>
        <el-button
          :type="stockForm.mode === 'IN' ? 'primary' : 'warning'"
          :loading="stockSubmitting"
          @click="submitStock"
        >
          确认{{ stockForm.mode === 'IN' ? '入库' : '出库' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.low-hint {
  padding: 2px 8px;
  border-radius: 999px;
  color: #b42318;
  background: #fef3f2;
  font-size: 10.5px;
}

.stock-number {
  color: #2f4a5c;
  font-size: 16px;
  font-weight: 700;
}

.stock-number.low {
  color: #d14343;
}

.unit-text {
  margin-left: 4px;
  color: #98a6b2;
  font-size: 10px;
}

:deep(.low-stock-row) {
  background: #fffafa;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 14px;
}

.field-hint {
  margin-top: 5px;
  color: #97a5b1;
  font-size: 10.5px;
  line-height: 1.5;
}

.stock-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
  padding: 14px 15px;
  border: 1px solid #e4ecef;
  border-radius: 11px;
  background: #f9fbfc;
}

.stock-summary > div:first-child {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stock-summary .muted {
  font-size: 10px;
}

.stock-summary b {
  color: #2e485a;
  font-size: 14px;
}

.stock-now {
  display: flex;
  align-items: baseline;
  gap: 5px;
  color: #899aa7;
  font-size: 10px;
}

.stock-now b {
  color: #0f766e;
  font-size: 23px;
}

.stock-now small {
  color: #a1aeba;
  font-size: 9px;
}

.overdraw-tip {
  margin-top: -4px;
  padding: 8px 10px;
  border-radius: 7px;
  color: #b54708;
  background: #fffaeb;
  font-size: 10.5px;
}

@media (max-width: 560px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
