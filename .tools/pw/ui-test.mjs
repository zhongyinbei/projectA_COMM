import { chromium } from 'playwright-core'
import { mkdirSync } from 'node:fs'
import assert from 'node:assert/strict'

const BASE = 'http://localhost:5173'
const SHOTS = 'D:/zhongyinbei/.tools/screens'
mkdirSync(SHOTS, { recursive: true })

const browser = await chromium.launch({
  executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe',
  headless: true,
  args: ['--disable-gpu', '--no-first-run']
})

const results = []
const errors = []

function record(name, detail = '') {
  results.push({ name, detail })
  console.log(`PASS ${name}${detail ? ` :: ${detail}` : ''}`)
}

async function newPage() {
  const context = await browser.newContext({ viewport: { width: 1440, height: 1000 }, deviceScaleFactor: 1 })
  const page = await context.newPage()
  page.on('pageerror', (error) => errors.push(`pageerror: ${error.message}`))
  page.on('console', (message) => {
    if (message.type() === 'error') errors.push(`console: ${message.text()}`)
  })
  return { context, page }
}

async function login(page, username, password, expectedPath) {
  await page.goto(`${BASE}/login`)
  await page.getByPlaceholder('请输入用户名').fill(username)
  await page.getByPlaceholder('请输入登录密码').fill(password)
  await page.getByRole('button', { name: '登录平台' }).click()
  await page.waitForURL((url) => url.pathname === expectedPath, { timeout: 20000 })
  await page.waitForLoadState('networkidle')
}

async function shot(page, name) {
  await page.screenshot({ path: `${SHOTS}/${name}.png`, fullPage: true })
}

{
  const { context, page } = await newPage()
  await page.goto(`${BASE}/login`)
  await page.waitForLoadState('networkidle')
  await shot(page, 'login')
  await login(page, 'admin', 'admin123', '/admin/dashboard')
  await page.waitForTimeout(1600)
  assert.equal(await page.locator('.chart-box canvas').count(), 4)
  assert.equal(await page.locator('.stock-alert').isVisible(), true)
  await shot(page, 'admin-dashboard')
  record('A7 管理员看板 4 张 ECharts 与红色告警', '4 canvas + stock-alert')

  await page.goto(`${BASE}/admin/equipment`)
  await page.waitForLoadState('networkidle')
  const equipmentRows = await page.locator('.el-table__body tbody tr').count()
  assert.ok(equipmentRows >= 5)
  await shot(page, 'admin-equipment')
  record('管理员设备台账渲染', `${equipmentRows} 行`)

  await page.goto(`${BASE}/admin/consumable`)
  await page.waitForLoadState('networkidle')
  assert.equal(await page.locator('.low-stock-row').count() >= 1, true)
  await shot(page, 'admin-consumable')
  record('管理员耗材库存与告警行', 'low-stock-row 可见')

  await page.goto(`${BASE}/admin/inspection-point`)
  await page.waitForLoadState('networkidle')
  await page.getByRole('button', { name: '查看二维码' }).first().click()
  await page.locator('.qr-stage img').waitFor({ state: 'visible', timeout: 10000 })
  const naturalWidth = await page.locator('.qr-stage img').evaluate((img) => img.naturalWidth)
  assert.equal(naturalWidth, 300)
  await page.waitForTimeout(400)
  await shot(page, 'admin-qr')
  record('管理员巡检点二维码 300px', `naturalWidth=${naturalWidth}`)
  await context.close()
}

{
  const { context, page } = await newPage()
  await login(page, 'reporter1', '123456', '/reporter/home')
  await page.waitForLoadState('networkidle')
  await shot(page, 'reporter-home')
  record('A1 报修者登录跳转', page.url())

  await page.goto(`${BASE}/reporter/repair-new`)
  await page.waitForLoadState('networkidle')
  await page.locator('.el-select').first().click()
  await page.locator('.el-select-dropdown__item').filter({ hasText: 'LAB-PC-001' }).first().click()
  await page.locator('textarea').first().fill('浏览器验收：投影仪画面闪烁，疑似信号线接触不良。')
  await page.locator('input[type=file]').setInputFiles('D:/zhongyinbei/.tools/qr.png')
  await page.locator('.image-preview img').waitFor({ state: 'visible', timeout: 15000 })
  await page.waitForTimeout(500)
  await page.getByRole('button', { name: '提交报修' }).click()
  await page.waitForURL((url) => url.pathname === '/reporter/my-orders', { timeout: 15000 })
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(500)
  const pendingRows = await page.locator('.el-tag', { hasText: '待接单' }).count()
  assert.ok(pendingRows >= 1)
  await shot(page, 'reporter-myorders')
  record('A2 报修者提交带图片报修并显示 PENDING', `${pendingRows} 条待接单`)

  await page.goto(`${BASE}/admin/dashboard`)
  await page.waitForURL((url) => url.pathname === '/reporter/home', { timeout: 10000 })
  record('A1 报修者越权访问管理端被拦截', page.url())

  await page.evaluate(() => {
    const user = JSON.parse(localStorage.getItem('lab_user'))
    user.role = 'ADMIN'
    localStorage.setItem('lab_user', JSON.stringify(user))
  })
  await page.goto(`${BASE}/admin/dashboard`)
  await page.waitForURL((url) => url.pathname === '/login', { timeout: 10000 })
  const tokenCleared = await page.evaluate(() => !localStorage.getItem('lab_token'))
  assert.equal(tokenCleared, true)
  await shot(page, 'login-tamper-redirect')
  record('A1 本地角色篡改被清除并跳回登录', page.url())
  await context.close()
}

{
  const { context, page } = await newPage()
  await login(page, 'repairer1', '123456', '/repairer/orders')
  await page.waitForLoadState('networkidle')
  await page.getByRole('button', { name: '接单', exact: true }).first().click()
  await page.locator('.el-message-box').getByRole('button', { name: '接单', exact: true }).click()
  await page.waitForTimeout(1200)

  await page.getByRole('button', { name: /我的工单/ }).click()
  await page.waitForTimeout(500)
  await page.getByRole('button', { name: '完成维修' }).first().click()
  await page.locator('.el-dialog textarea').fill('浏览器验收：重新固定信号线，画面恢复正常。')
  await page.locator('.el-dialog').getByRole('button', { name: '确认完成' }).click()
  await page.waitForTimeout(1500)
  await shot(page, 'repairer-completed')
  record('A3 维修员接单并完成工单', '状态已刷新')

  await page.goto(`${BASE}/repairer/inspection`)
  await page.waitForLoadState('networkidle')
  await context.setOffline(true)
  await page.waitForTimeout(500)

  await page.getByPlaceholder('例如 P-001').fill('P-002')
  await page.locator('textarea').first().fill('离线验收第一条')
  await page.getByRole('button', { name: '保存到本地' }).click()
  await page.waitForTimeout(400)
  await page.locator('.quick-points').getByRole('button', { name: 'P-003' }).click()
  await page.locator('.result-group').getByText('异常', { exact: true }).click()
  await page.locator('textarea').first().fill('离线验收第二条：网线松动')
  await page.getByRole('button', { name: '保存到本地' }).click()
  await page.waitForTimeout(500)
  assert.equal((await page.locator('.pending-stat b').innerText()).trim(), '2')
  await shot(page, 'repairer-offline')
  record('A5 断网提交 2 条巡检记录', '本地待同步 2 条')

  await context.setOffline(false)
  await page.waitForTimeout(4500)
  const pendingNow = (await page.locator('.pending-stat b').innerText()).trim()
  assert.equal(pendingNow, '0')
  await page.waitForTimeout(800)
  const offlineTags = await page.locator('.el-tag', { hasText: '离线同步' }).count()
  assert.ok(offlineTags >= 2)
  await shot(page, 'repairer-inspection')
  record('A5 恢复网络自动同步离线巡检', `待同步 ${pendingNow}，离线记录标签 ${offlineTags}`)
  await context.close()
}

await browser.close()

console.log('\n--- PAGE ERRORS ---')
console.log(errors.length ? errors.join('\n') : 'NONE')
console.log(`SCREENSHOTS ${SHOTS}`)
assert.equal(errors.length, 0, `页面存在错误: ${errors.join('; ')}`)
console.log('ALL_UI_TESTS_PASSED')
