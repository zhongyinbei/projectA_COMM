<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore, homePath } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const accounts = [
  { role: '管理员', username: 'admin', password: 'admin123', note: '设备 · 耗材 · 看板' },
  { role: '维修员', username: 'repairer1', password: '123456', note: '接单 · 巡检' },
  { role: '报修者', username: 'reporter1', password: '123456', note: '报修 · 查进度' }
]

function fillAccount(account) {
  form.username = account.username
  form.password = account.password
  formRef.value?.clearValidate()
}

async function handleLogin() {
  if (loading.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    const user = await auth.login({ username: form.username.trim(), password: form.password })
    ElMessage.success(`欢迎回来，${user.realName}`)
    await router.replace(homePath(user.role))
  } catch {
    /* 错误提示由请求拦截器统一处理 */
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (route.query.expired) {
    ElMessage.warning('登录已过期，请重新登录')
  } else if (route.query.reason === 'forbidden') {
    ElMessage.warning('当前账号无权访问该页面')
  } else if (route.query.reason === 'invalid') {
    ElMessage.warning('登录身份校验失败，请重新登录')
  }
})
</script>

<template>
  <div class="login-page">
    <section class="login-visual">
      <div class="visual-top">
        <div class="mini-brand">
          <span class="mini-mark">LC</span>
          <span>LabCare / 实验实训中心</span>
        </div>
        <span class="edition">OPS · 2026</span>
      </div>

      <div class="hero">
        <div class="eyebrow"><span class="pulse"></span> 实训室设备巡检与耗材智能管理平台</div>
        <h1>让设备状态<br /><span>清晰可见</span>。</h1>
        <p class="hero-desc">
          从一张报修单、一次巡检记录，到每一件耗材的出库与预警。
          把分散的实验室日常，收进同一条可追踪的工作流。
        </p>
        <div class="feature-row">
          <div class="feature">
            <b>01</b>
            <span>报修闭环</span>
            <small>提交 → 接单 → 完成</small>
          </div>
          <div class="feature">
            <b>02</b>
            <span>离线巡检</span>
            <small>断网记录 · 恢复同步</small>
          </div>
          <div class="feature">
            <b>03</b>
            <span>库存预警</span>
            <small>出库联动 · 阈值提醒</small>
          </div>
        </div>
      </div>

      <div class="visual-card">
        <div class="card-head">
          <span class="live-dot"></span>
          <span>设备运行概览</span>
          <span class="card-time">实时</span>
        </div>
        <div class="card-main">
          <div>
            <div class="card-label">A301 · 教学投影仪</div>
            <div class="card-value">运行正常</div>
          </div>
          <div class="ring">
            <span>98<small>%</small></span>
            <i>可用率</i>
          </div>
        </div>
        <div class="card-bars">
          <span style="width: 86%"></span>
          <span style="width: 62%"></span>
          <span style="width: 74%"></span>
          <span style="width: 48%"></span>
        </div>
        <div class="card-foot">
          <span>巡检覆盖 3 个点位</span>
          <span>耗材预警 1 项</span>
        </div>
      </div>

      <div class="visual-foot">
        <span>LAB OPERATIONS SYSTEM</span>
        <span>设备有迹 · 耗材有数 · 巡检有据</span>
      </div>
    </section>

    <section class="login-panel">
      <div class="form-wrap">
        <div class="mobile-brand">
          <span class="mini-mark">LC</span>
          <span>LabCare</span>
        </div>
        <div class="form-kicker">SIGN IN</div>
        <h2>欢迎回来</h2>
        <p class="form-desc">使用实验实训中心分配的账号登录工作台</p>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="handleLogin">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" size="large" placeholder="请输入用户名" autocomplete="username" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              size="large"
              type="password"
              show-password
              placeholder="请输入登录密码"
              autocomplete="current-password"
            />
          </el-form-item>
          <el-button
            type="primary"
            size="large"
            class="submit-btn"
            :loading="loading"
            native-type="button"
            @click="handleLogin"
          >
            {{ loading ? '正在登录…' : '登录平台' }}
          </el-button>
        </el-form>

        <div class="demo-box">
          <div class="demo-head">
            <span>演示账号</span>
            <small>点击自动填充</small>
          </div>
          <div class="demo-list">
            <button
              v-for="account in accounts"
              :key="account.username"
              type="button"
              class="demo-item"
              @click="fillAccount(account)"
            >
              <span class="demo-role">{{ account.role }}</span>
              <span class="demo-account">{{ account.username }}</span>
              <span class="demo-note">{{ account.note }}</span>
            </button>
          </div>
        </div>

        <p class="copyright">Demo Environment · 数据仅用于功能演示</p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.login-page {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(420px, 0.92fr);
  min-height: 100vh;
  background: #f5f7f8;
}

.login-visual {
  position: relative;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 34px 56px 30px;
  color: #e6f1f3;
  background:
    radial-gradient(circle at 82% 14%, rgba(45, 212, 191, 0.2), transparent 28%),
    radial-gradient(circle at 8% 88%, rgba(245, 158, 11, 0.12), transparent 28%),
    linear-gradient(150deg, #07131f 0%, #0b1d2c 54%, #0d2930 100%);
}

.login-visual::before {
  content: "";
  position: absolute;
  inset: 0;
  opacity: 0.34;
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.08) 1px, transparent 1px);
  background-size: 46px 46px;
  mask-image: linear-gradient(to bottom, black, transparent 88%);
}

.login-visual::after {
  content: "";
  position: absolute;
  right: -120px;
  bottom: -160px;
  width: 420px;
  height: 420px;
  border: 1px solid rgba(94, 234, 212, 0.16);
  border-radius: 50%;
  box-shadow: 0 0 0 70px rgba(94, 234, 212, 0.025), 0 0 0 140px rgba(94, 234, 212, 0.018);
}

.visual-top,
.hero,
.visual-card,
.visual-foot {
  position: relative;
  z-index: 1;
}

.visual-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.mini-brand {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 12px;
  letter-spacing: 0.4px;
}

.mini-mark {
  display: grid;
  place-items: center;
  width: 31px;
  height: 31px;
  border-radius: 9px;
  color: #042f2e;
  background: #5eead4;
  font-size: 11px;
  font-weight: 800;
}

.edition {
  color: #7f98a9;
  font-size: 10px;
  letter-spacing: 1.8px;
}

.hero {
  max-width: 600px;
  margin-top: auto;
  padding-top: 68px;
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 11px;
  border: 1px solid rgba(94, 234, 212, 0.2);
  border-radius: 999px;
  color: #a7c7c5;
  background: rgba(15, 118, 110, 0.12);
  font-size: 11px;
  letter-spacing: 0.5px;
}

.pulse {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #5eead4;
  box-shadow: 0 0 0 5px rgba(94, 234, 212, 0.1);
}

.hero h1 {
  margin: 24px 0 18px;
  color: #f2f8f9;
  font-size: clamp(42px, 4.6vw, 62px);
  font-weight: 700;
  line-height: 1.13;
  letter-spacing: -2.6px;
}

.hero h1 span {
  color: #5eead4;
  background: linear-gradient(100deg, #99f6e4, #5eead4 55%, #fcd34d);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.hero-desc {
  max-width: 500px;
  margin: 0;
  color: #92a8b8;
  font-size: 14px;
  line-height: 1.9;
}

.feature-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 34px;
  padding-top: 22px;
  border-top: 1px solid rgba(148, 163, 184, 0.16);
}

.feature {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.feature b {
  color: #5eead4;
  font-family: "Cascadia Mono", Consolas, monospace;
  font-size: 10px;
  font-weight: 500;
  letter-spacing: 1px;
}

.feature span {
  color: #dce8eb;
  font-size: 13px;
  font-weight: 600;
}

.feature small {
  color: #738b9b;
  font-size: 10.5px;
}

.visual-card {
  width: min(100%, 466px);
  margin: 42px 0 28px;
  padding: 16px 18px 14px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 16px;
  background: linear-gradient(150deg, rgba(255, 255, 255, 0.075), rgba(255, 255, 255, 0.025));
  box-shadow: 0 24px 50px rgba(2, 8, 15, 0.22);
  backdrop-filter: blur(6px);
}

.card-head {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #a8bdc9;
  font-size: 11px;
}

.live-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #4ade80;
}

.card-time {
  margin-left: auto;
  color: #688092;
  font-size: 10px;
}

.card-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin: 15px 0 12px;
}

.card-label {
  color: #7f98a9;
  font-size: 11px;
}

.card-value {
  margin-top: 5px;
  color: #f0fdfa;
  font-size: 23px;
  font-weight: 650;
  letter-spacing: -0.6px;
}

.ring {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  flex-shrink: 0;
  border: 1px solid rgba(94, 234, 212, 0.45);
  border-radius: 50%;
  background: rgba(15, 118, 110, 0.13);
}

.ring span {
  color: #5eead4;
  font-size: 19px;
  font-weight: 700;
  line-height: 1;
}

.ring small {
  font-size: 9px;
}

.ring i {
  margin-top: 3px;
  color: #8ca6b4;
  font-size: 8px;
  font-style: normal;
}

.card-bars {
  display: flex;
  align-items: flex-end;
  gap: 5px;
  height: 29px;
}

.card-bars span {
  display: block;
  height: 100%;
  max-width: 25%;
  border-radius: 3px 3px 0 0;
  background: linear-gradient(to top, rgba(45, 212, 191, 0.16), rgba(94, 234, 212, 0.65));
}

.card-bars span:nth-child(2) {
  opacity: 0.62;
}

.card-bars span:nth-child(3) {
  opacity: 0.8;
}

.card-bars span:nth-child(4) {
  opacity: 0.42;
}

.card-foot {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 11px;
  color: #708898;
  font-size: 9.5px;
}

.visual-foot {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-top: auto;
  padding-top: 18px;
  border-top: 1px solid rgba(148, 163, 184, 0.12);
  color: #597182;
  font-size: 9px;
  letter-spacing: 1.1px;
}

.login-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 44px 40px;
  background:
    radial-gradient(circle at 92% 8%, rgba(15, 118, 110, 0.055), transparent 26%),
    #f7f9fa;
}

.form-wrap {
  width: min(100%, 382px);
}

.mobile-brand {
  display: none;
}

.form-kicker {
  color: #0f766e;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 2px;
}

.form-wrap h2 {
  margin: 8px 0 7px;
  color: #102a43;
  font-size: 30px;
  letter-spacing: -1px;
}

.form-desc {
  margin: 0 0 28px;
  color: #7b8b9c;
  font-size: 13px;
}

.form-wrap :deep(.el-form-item) {
  margin-bottom: 19px;
}

.form-wrap :deep(.el-form-item__label) {
  color: #44586b;
  font-size: 12px;
  font-weight: 600;
}

.form-wrap :deep(.el-input__wrapper) {
  padding: 3px 13px;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 0 0 1px #dfe8ee inset;
}

.form-wrap :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #0f766e inset, 0 0 0 4px rgba(15, 118, 110, 0.08);
}

.submit-btn {
  width: 100%;
  height: 45px;
  margin-top: 4px;
  border-radius: 10px;
  font-size: 14px;
  letter-spacing: 0.4px;
  box-shadow: 0 9px 22px rgba(15, 118, 110, 0.18);
}

.demo-box {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px dashed #dbe5eb;
}

.demo-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  color: #56697b;
  font-size: 11px;
  font-weight: 600;
}

.demo-head small {
  color: #9aa9b6;
  font-weight: 400;
}

.demo-list {
  display: grid;
  gap: 7px;
}

.demo-item {
  display: grid;
  grid-template-columns: 54px 82px 1fr;
  align-items: center;
  gap: 8px;
  padding: 9px 10px;
  border: 1px solid #e4ebf0;
  border-radius: 9px;
  color: #5c7082;
  background: rgba(255, 255, 255, 0.6);
  text-align: left;
  cursor: pointer;
  transition: 0.16s ease;
}

.demo-item:hover {
  border-color: #a7d4ce;
  background: #f1faf8;
  transform: translateY(-1px);
}

.demo-role {
  color: #0f766e;
  font-size: 11px;
  font-weight: 600;
}

.demo-account {
  color: #263f54;
  font-family: "Cascadia Mono", Consolas, monospace;
  font-size: 11px;
}

.demo-note {
  color: #97a6b3;
  font-size: 10px;
  text-align: right;
}

.copyright {
  margin: 28px 0 0;
  color: #a3b0bb;
  font-size: 10px;
  text-align: center;
  letter-spacing: 0.3px;
}

@media (max-width: 1080px) {
  .login-visual {
    padding: 28px 36px;
  }

  .hero h1 {
    font-size: 44px;
    letter-spacing: -1.8px;
  }

  .visual-card {
    display: none;
  }

  .visual-foot {
    margin-top: 40px;
  }
}

@media (max-width: 860px) {
  .login-page {
    display: block;
    min-height: 100vh;
  }

  .login-visual {
    min-height: 0;
    padding: 24px 24px 30px;
  }

  .visual-top {
    margin-bottom: 34px;
  }

  .hero {
    padding-top: 0;
  }

  .hero h1 {
    margin: 18px 0 12px;
    font-size: 36px;
    letter-spacing: -1.4px;
  }

  .hero-desc,
  .feature-row,
  .visual-foot {
    display: none;
  }

  .login-panel {
    min-height: calc(100vh - 250px);
    align-items: flex-start;
    padding: 34px 22px 50px;
  }

  .form-wrap {
    width: min(100%, 430px);
    margin: 0 auto;
  }

  .mobile-brand {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 26px;
    color: #274256;
    font-size: 14px;
    font-weight: 700;
  }
}

@media (max-width: 480px) {
  .hero h1 {
    font-size: 31px;
  }

  .edition {
    display: none;
  }

  .demo-item {
    grid-template-columns: 52px 1fr;
  }

  .demo-note {
    display: none;
  }
}
</style>
