<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'
import { getEquipments } from '../../api/equipment'
import { createRepairOrder, uploadImage } from '../../api/repairOrder'

const router = useRouter()
const auth = useAuthStore()
const formRef = ref()
const equipments = ref([])
const loading = ref(false)
const uploading = ref(false)
const imageUrl = ref('')
const fileInputKey = ref(0)

const form = reactive({
  equipmentId: null,
  description: ''
})

const rules = {
  equipmentId: [{ required: true, message: '请选择需要报修的设备', trigger: 'change' }],
  description: [
    { required: true, message: '请描述设备故障现象', trigger: 'blur' },
    { min: 5, message: '请至少填写 5 个字，方便维修员判断问题', trigger: 'blur' }
  ]
}

const selected = computed(() => equipments.value.find((item) => item.id === form.equipmentId))
const statusText = {
  NORMAL: '正常',
  REPAIRING: '维修中',
  SCRAPPED: '已报废'
}

async function loadEquipments() {
  const data = await getEquipments({ page: 1, size: 100 })
  equipments.value = data.records || []
}

function beforeUpload(file) {
  const allowed = ['image/png', 'image/jpeg', 'image/gif', 'image/webp']
  if (!allowed.includes(file.type)) {
    ElMessage.error('仅支持 png、jpg、jpeg、gif、webp 图片')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

async function handleUpload(options) {
  uploading.value = true
  try {
    const data = await uploadImage(options.file)
    imageUrl.value = data.url
    ElMessage.success('现场图片上传成功')
  } catch {
    /* 错误提示由请求拦截器处理 */
  } finally {
    uploading.value = false
  }
}

function removeImage() {
  imageUrl.value = ''
  fileInputKey.value += 1
}

async function submit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    await createRepairOrder({
      equipmentId: form.equipmentId,
      description: form.description.trim(),
      imageUrl: imageUrl.value || null
    })
    ElMessage.success('报修已提交，维修员会尽快处理')
    router.push('/reporter/my-orders')
  } catch {
    /* 错误提示由请求拦截器处理 */
  } finally {
    loading.value = false
  }
}

onMounted(loadEquipments)
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2>发起报修</h2>
        <p>选择设备并描述故障现象，必要时上传现场图片。</p>
      </div>
      <div class="head-actions">
        <el-button text @click="router.push('/reporter/my-orders')">返回我的工单</el-button>
      </div>
    </div>

    <div class="split-grid">
      <section class="panel panel-pad">
        <div class="form-head">
          <div>
            <div class="step-kicker">NEW REPAIR REQUEST</div>
            <h3>填写报修信息</h3>
          </div>
          <span class="reporter-chip">{{ auth.user?.realName }}</span>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="repair-form">
          <el-form-item label="报修设备" prop="equipmentId">
            <el-select
              v-model="form.equipmentId"
              filterable
              size="large"
              placeholder="搜索资产编号或设备名称"
              style="width: 100%"
            >
              <el-option
                v-for="item in equipments"
                :key="item.id"
                :label="`${item.assetNo} · ${item.name}`"
                :value="item.id"
                :disabled="item.status === 'SCRAPPED'"
              >
                <div class="option-row">
                  <span>{{ item.assetNo }} · {{ item.name }}</span>
                  <small>{{ item.location }}</small>
                </div>
              </el-option>
            </el-select>
          </el-form-item>

          <div v-if="selected" class="selected-card">
            <div>
              <span class="muted">当前设备</span>
              <b>{{ selected.name }}</b>
              <small>{{ selected.assetNo }} · {{ selected.location }}</small>
            </div>
            <el-tag :type="selected.status === 'REPAIRING' ? 'warning' : 'success'" effect="plain" round>
              {{ statusText[selected.status] || selected.status }}
            </el-tag>
          </div>

          <el-form-item label="故障描述" prop="description">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="6"
              maxlength="500"
              show-word-limit
              resize="none"
              placeholder="请描述故障现象、发生时间，以及已经尝试过的处理方式…"
            />
          </el-form-item>

          <el-form-item label="现场图片（选填）">
            <div v-if="imageUrl" class="image-preview">
              <el-image :src="imageUrl" fit="cover" :preview-src-list="[imageUrl]" preview-teleported />
              <button type="button" class="remove-image" @click="removeImage">×</button>
              <div class="preview-caption">已上传，点击图片可预览</div>
            </div>
            <el-upload
              v-else
              :key="fileInputKey"
              drag
              action="#"
              accept="image/png,image/jpeg,image/gif,image/webp"
              :show-file-list="false"
              :before-upload="beforeUpload"
              :http-request="handleUpload"
              :disabled="uploading"
              class="uploader"
            >
              <div class="upload-inner">
                <span class="upload-mark">＋</span>
                <b>{{ uploading ? '正在上传…' : '拖拽图片到这里，或点击选择' }}</b>
                <small>支持 png / jpg / jpeg / gif / webp，单张不超过 5MB</small>
              </div>
            </el-upload>
          </el-form-item>

          <div class="form-actions">
            <el-button @click="router.push('/reporter/home')">取消</el-button>
            <el-button type="primary" :loading="loading" @click="submit">提交报修</el-button>
          </div>
        </el-form>
      </section>

      <aside class="side-column">
        <section class="panel panel-pad process-panel">
          <div class="section-title">
            <h3>提交后会怎样？</h3>
            <span class="hint">处理流程</span>
          </div>
          <div class="process-list">
            <div class="process-item">
              <span class="process-dot">1</span>
              <div><b>进入待接单队列</b><p>维修员端会收到新的报修工单。</p></div>
            </div>
            <div class="process-item">
              <span class="process-dot">2</span>
              <div><b>维修员接单</b><p>设备状态自动标记为“维修中”，可查看维修员信息。</p></div>
            </div>
            <div class="process-item">
              <span class="process-dot">3</span>
              <div><b>完成并恢复</b><p>维修完成后设备状态恢复为“正常”，工单进入已完成。</p></div>
            </div>
          </div>
        </section>

        <section class="panel panel-pad note-panel">
          <div class="note-title">描述建议</div>
          <ul>
            <li>写清楚设备编号与所在实验室。</li>
            <li>说明是“无法开机”“画面异常”还是“异响”等具体现象。</li>
            <li>如已影响教学，请在描述中注明紧急程度。</li>
          </ul>
        </section>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.form-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 24px;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--line);
}

.step-kicker {
  color: #0f766e;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 1.8px;
}

.form-head h3 {
  margin: 7px 0 0;
  font-size: 20px;
  letter-spacing: -0.4px;
}

.reporter-chip {
  padding: 4px 10px;
  border-radius: 999px;
  color: #0f766e;
  background: #e9f6f2;
  font-size: 11px;
}

.repair-form :deep(.el-form-item__label) {
  color: #41566a;
  font-size: 12px;
  font-weight: 600;
}

.repair-form :deep(.el-form-item) {
  margin-bottom: 22px;
}

.option-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.option-row small {
  color: #95a4b1;
  font-size: 11px;
}

.selected-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin: -6px 0 20px;
  padding: 13px 15px;
  border: 1px solid #d9ebe6;
  border-radius: 11px;
  background: #f6fbf9;
}

.selected-card > div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.selected-card .muted {
  font-size: 10px;
  letter-spacing: 0.4px;
}

.selected-card b {
  color: #244154;
  font-size: 13px;
}

.selected-card small {
  color: #8293a2;
  font-size: 11px;
}

.uploader {
  width: 100%;
}

.uploader :deep(.el-upload) {
  width: 100%;
}

.uploader :deep(.el-upload-dragger) {
  width: 100%;
  padding: 24px 20px;
  border: 1px dashed #c9d8df;
  border-radius: 12px;
  background: #f9fbfc;
  transition: 0.16s ease;
}

.uploader :deep(.el-upload-dragger:hover) {
  border-color: #0f766e;
  background: #f4fbf9;
}

.upload-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.upload-mark {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  margin-bottom: 3px;
  border-radius: 10px;
  color: #0f766e;
  background: #e3f3ef;
  font-size: 20px;
  line-height: 1;
}

.upload-inner b {
  color: #4b6173;
  font-size: 12px;
  font-weight: 600;
}

.upload-inner small {
  color: #98a7b4;
  font-size: 10.5px;
}

.image-preview {
  position: relative;
  width: 220px;
  border: 1px solid #dce7ec;
  border-radius: 12px;
  overflow: hidden;
  background: #f8fafb;
}

.image-preview :deep(.el-image) {
  display: block;
  width: 100%;
  height: 140px;
}

.remove-image {
  position: absolute;
  top: 7px;
  right: 7px;
  width: 25px;
  height: 25px;
  border: none;
  border-radius: 50%;
  color: #fff;
  background: rgba(15, 23, 42, 0.68);
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
}

.preview-caption {
  padding: 7px 10px;
  color: #8797a5;
  font-size: 10px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 6px;
}

.side-column {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.process-list {
  display: grid;
  gap: 19px;
}

.process-item {
  display: flex;
  gap: 12px;
}

.process-dot {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  border-radius: 8px;
  color: #0f766e;
  background: #e9f6f2;
  font-family: "Cascadia Mono", Consolas, monospace;
  font-size: 10px;
  font-weight: 600;
}

.process-item b {
  color: #30495d;
  font-size: 12.5px;
}

.process-item p {
  margin: 4px 0 0;
  color: #8192a1;
  font-size: 11px;
  line-height: 1.65;
}

.note-panel {
  background: linear-gradient(160deg, #fbf7ee, #fff);
  border-color: #f0e3c8;
}

.note-title {
  color: #8a6420;
  font-size: 12px;
  font-weight: 700;
}

.note-panel ul {
  margin: 11px 0 0;
  padding-left: 16px;
  color: #8b7a5e;
  font-size: 11.5px;
  line-height: 1.85;
}

@media (max-width: 760px) {
  .image-preview {
    width: 100%;
  }
}
</style>
