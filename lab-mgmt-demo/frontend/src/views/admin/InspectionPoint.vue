<script setup>
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createInspectionPoint, getInspectionPointQr, getInspectionPoints } from '../../api/inspection'

const points = ref([])
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const qrDialog = ref(false)
const qrLoading = ref(false)
const qrUrl = ref('')
const qrPoint = ref(null)
const formRef = ref()
const form = reactive({ pointCode: '', location: '' })

const rules = {
  pointCode: [{ required: true, message: '请输入巡检点编码', trigger: 'blur' }],
  location: [{ required: true, message: '请输入巡检点位置', trigger: 'blur' }]
}

function formatDate(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 16)
}

async function load() {
  loading.value = true
  try {
    points.value = await getInspectionPoints()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.pointCode = ''
  form.location = ''
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
  try {
    await createInspectionPoint({
      pointCode: form.pointCode.trim(),
      location: form.location.trim()
    })
    ElMessage.success('巡检点已新增')
    dialogVisible.value = false
    await load()
  } catch {
    /* 错误提示由请求拦截器处理 */
  } finally {
    submitting.value = false
  }
}

function revokeQr() {
  if (qrUrl.value) {
    URL.revokeObjectURL(qrUrl.value)
    qrUrl.value = ''
  }
}

async function openQr(row) {
  qrPoint.value = row
  qrDialog.value = true
  qrLoading.value = true
  revokeQr()
  try {
    const blob = await getInspectionPointQr(row.id)
    qrUrl.value = URL.createObjectURL(blob)
  } catch {
    qrDialog.value = false
  } finally {
    qrLoading.value = false
  }
}

onMounted(load)
onUnmounted(revokeQr)
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2>巡检点管理</h2>
        <p>每个巡检点对应一张二维码，维修员输入编码或扫码完成巡检。</p>
      </div>
      <div class="head-actions">
        <el-button type="primary" @click="openCreate">新增巡检点</el-button>
      </div>
    </div>

    <div class="point-overview">
      <div class="overview-mark">◎</div>
      <div>
        <b>{{ points.length }} 个巡检点</b>
        <span>覆盖实训室关键设备与弱电区域</span>
      </div>
      <div class="overview-flow">
        <span>生成二维码</span><i>→</i><span>现场巡检</span><i>→</i><span>记录可追溯</span>
      </div>
    </div>

    <section class="panel">
      <div class="toolbar">
        <div class="grow">
          <div class="section-title" style="margin: 0">
            <h3>巡检点列表</h3>
            <span class="hint">共 {{ points.length }} 个</span>
          </div>
        </div>
        <span class="toolbar-tip">二维码内容为巡检点编码</span>
        <el-button text @click="load">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="points" style="width: 100%">
        <el-table-column label="巡检点编码" width="150">
          <template #default="{ row }"><span class="mono point-code">{{ row.pointCode }}</span></template>
        </el-table-column>
        <el-table-column prop="location" label="所在位置" min-width="240" />
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }"><span class="muted">{{ formatDate(row.createdAt) }}</span></template>
        </el-table-column>
        <el-table-column label="二维码" width="170" fixed="right">
          <template #default="{ row }">
            <el-button plain size="small" @click="openQr(row)">查看二维码</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-copy">暂无巡检点，点击“新增巡检点”开始配置。</div>
        </template>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" title="新增巡检点" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="巡检点编码" prop="pointCode">
          <el-input v-model="form.pointCode" maxlength="50" placeholder="例如 P-004" />
          <div class="field-hint">编码将写入二维码，建议使用 P-xxx 格式。</div>
        </el-form-item>
        <el-form-item label="所在位置" prop="location">
          <el-input v-model="form.location" maxlength="100" placeholder="例如 B203 物联网实训室" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="save">新增巡检点</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="qrDialog" title="巡检点二维码" width="410px" @closed="revokeQr">
      <div class="qr-card">
        <div class="qr-head">
          <div>
            <span class="muted">巡检点</span>
            <b>{{ qrPoint?.pointCode }}</b>
          </div>
          <span class="qr-location">{{ qrPoint?.location }}</span>
        </div>
        <div v-loading="qrLoading" class="qr-stage">
          <img v-if="qrUrl" :src="qrUrl" alt="巡检点二维码" />
          <span v-else-if="!qrLoading" class="muted">二维码加载失败</span>
        </div>
        <p class="qr-tip">二维码内容为 <code>{{ qrPoint?.pointCode }}</code>，尺寸 300 × 300。</p>
        <a v-if="qrUrl" :href="qrUrl" :download="`${qrPoint?.pointCode}.png`" class="download-link">下载二维码图片</a>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.point-overview {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 16px;
  padding: 18px 22px;
  border: 1px solid #d9ebe6;
  border-radius: 14px;
  background:
    radial-gradient(circle at 90% 0%, rgba(45, 212, 191, 0.12), transparent 30%),
    linear-gradient(120deg, #f6fcfa, #fff);
}

.overview-mark {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 11px;
  color: #0f766e;
  background: #e4f4f0;
  font-size: 21px;
}

.point-overview > div:nth-child(2) {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.point-overview b {
  color: #294b5c;
  font-size: 14px;
}

.point-overview span {
  color: #8597a5;
  font-size: 10.5px;
}

.overview-flow {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-left: auto;
  color: #7f95a2;
  font-size: 10.5px;
}

.overview-flow i {
  color: #a9c4bd;
  font-style: normal;
}

.point-code {
  color: #0f766e;
  font-weight: 600;
}

.field-hint {
  margin-top: 5px;
  color: #98a6b2;
  font-size: 10.5px;
}

.qr-card {
  padding: 2px 2px 4px;
}

.qr-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  padding: 2px 3px 14px;
}

.qr-head > div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.qr-head .muted {
  font-size: 10px;
}

.qr-head b {
  color: #0f766e;
  font-family: "Cascadia Mono", Consolas, monospace;
  font-size: 18px;
}

.qr-location {
  color: #7d8f9d;
  font-size: 10.5px;
  text-align: right;
}

.qr-stage {
  display: grid;
  place-items: center;
  min-height: 280px;
  padding: 16px;
  border: 1px solid #e4ecef;
  border-radius: 13px;
  background:
    linear-gradient(45deg, #f8fafb 25%, transparent 25%) 0 0 / 14px 14px,
    linear-gradient(-45deg, #f8fafb 25%, transparent 25%) 0 7px / 14px 14px,
    #fff;
}

.qr-stage img {
  width: 240px;
  height: 240px;
  border: 1px solid #e8eef1;
  border-radius: 8px;
  background: #fff;
}

.qr-tip {
  margin: 13px 0 10px;
  color: #8394a2;
  font-size: 10.5px;
  text-align: center;
}

.qr-tip code {
  padding: 1px 5px;
  border-radius: 3px;
  color: #0f766e;
  background: #eaf6f3;
}

.download-link {
  display: block;
  color: #0f766e;
  font-size: 11px;
  text-align: center;
}

@media (max-width: 700px) {
  .point-overview {
    align-items: flex-start;
  }

  .overview-flow {
    display: none;
  }
}
</style>
