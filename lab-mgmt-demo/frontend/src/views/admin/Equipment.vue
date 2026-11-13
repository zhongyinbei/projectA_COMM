<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createEquipment,
  deleteEquipment,
  getEquipments,
  updateEquipment
} from '../../api/equipment'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const filters = reactive({ keyword: '', status: '' })
const form = reactive({
  id: null,
  assetNo: '',
  name: '',
  location: '',
  category: '',
  status: 'NORMAL'
})

const rules = {
  assetNo: [{ required: true, message: '请输入资产编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  location: [{ required: true, message: '请输入设备位置', trigger: 'blur' }]
}

const statusMeta = {
  NORMAL: { label: '正常', type: 'success' },
  REPAIRING: { label: '维修中', type: 'warning' },
  SCRAPPED: { label: '已报废', type: 'info' }
}

function formatDate(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 16)
}

async function load() {
  loading.value = true
  try {
    const data = await getEquipments({
      page: page.value,
      size: size.value,
      keyword: filters.keyword.trim() || undefined,
      status: filters.status || undefined
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
  filters.keyword = ''
  filters.status = ''
  page.value = 1
  load()
}

function openCreate() {
  Object.assign(form, { id: null, assetNo: '', name: '', location: '', category: '', status: 'NORMAL' })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    assetNo: row.assetNo,
    name: row.name,
    location: row.location,
    category: row.category || '',
    status: row.status
  })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function save() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  const payload = {
    assetNo: form.assetNo.trim(),
    name: form.name.trim(),
    location: form.location.trim(),
    category: form.category.trim() || null,
    status: form.status
  }
  try {
    if (form.id) {
      await updateEquipment(form.id, payload)
      ElMessage.success('设备信息已更新')
    } else {
      await createEquipment(payload)
      ElMessage.success('设备已新增')
    }
    dialogVisible.value = false
    await load()
  } catch {
    /* 错误提示由请求拦截器处理 */
  } finally {
    submitting.value = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除设备“${row.name}”（${row.assetNo}）吗？存在工单引用的设备无法删除。`,
      '删除设备',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await deleteEquipment(row.id)
    ElMessage.success('设备已删除')
    if (rows.value.length === 1 && page.value > 1) page.value -= 1
    await load()
  } catch {
    /* 错误提示由请求拦截器处理 */
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
        <h2>设备台账</h2>
        <p>维护实训室设备资产信息，设备状态会随报修工单自动变化。</p>
      </div>
      <div class="head-actions">
        <el-button type="primary" @click="openCreate">新增设备</el-button>
      </div>
    </div>

    <section class="panel">
      <div class="toolbar">
        <el-input
          v-model="filters.keyword"
          placeholder="搜索资产编号 / 名称 / 位置"
          clearable
          style="width: 260px"
          @keyup.enter="search"
        />
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 140px" @change="search">
          <el-option label="正常" value="NORMAL" />
          <el-option label="维修中" value="REPAIRING" />
          <el-option label="已报废" value="SCRAPPED" />
        </el-select>
        <el-button type="primary" plain @click="search">查询</el-button>
        <el-button text @click="reset">重置</el-button>
        <span class="grow"></span>
        <span class="toolbar-tip">共 {{ total }} 台设备</span>
        <el-button text @click="load">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" style="width: 100%">
        <el-table-column label="资产编号" width="145">
          <template #default="{ row }"><span class="mono asset-no">{{ row.assetNo }}</span></template>
        </el-table-column>
        <el-table-column prop="name" label="设备名称" min-width="170">
          <template #default="{ row }">
            <div class="table-title">{{ row.name }}</div>
            <div class="table-sub">{{ row.category || '未分类' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="所在位置" min-width="190" />
        <el-table-column label="状态" width="105">
          <template #default="{ row }">
            <el-tag :type="statusMeta[row.status]?.type || 'info'" effect="light" round>
              {{ statusMeta[row.status]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="155">
          <template #default="{ row }"><span class="muted">{{ formatDate(row.createdAt) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-copy">暂无设备，点击“新增设备”建立台账。</div>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑设备' : '新增设备'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="资产编号" prop="assetNo">
            <el-input v-model="form.assetNo" maxlength="50" placeholder="例如 LAB-PC-006" />
          </el-form-item>
          <el-form-item label="设备名称" prop="name">
            <el-input v-model="form.name" maxlength="100" placeholder="例如 教学投影仪" />
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="所在位置" prop="location">
            <el-input v-model="form.location" maxlength="100" placeholder="例如 A301 多媒体教室" />
          </el-form-item>
          <el-form-item label="设备分类">
            <el-input v-model="form.category" maxlength="50" placeholder="例如 显示设备" />
          </el-form-item>
        </div>
        <el-form-item label="设备状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="正常" value="NORMAL" />
            <el-option label="维修中" value="REPAIRING" />
            <el-option label="已报废" value="SCRAPPED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="save">{{ form.id ? '保存修改' : '新增设备' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.asset-no {
  color: #0f766e;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 14px;
}

@media (max-width: 560px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
