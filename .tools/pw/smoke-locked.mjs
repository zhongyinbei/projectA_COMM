import { chromium } from 'playwright-core'
import assert from 'node:assert/strict'

const BASE = 'http://localhost:5173'
const browser = await chromium.launch({
  executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe',
  headless: true,
  args: ['--disable-gpu', '--no-first-run']
})
const errors = []

async function pageFor(username, password, expectedPath) {
  const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
  const page = await context.newPage()
  page.on('pageerror', (error) => errors.push(`pageerror ${username}: ${error.message}`))
  page.on('console', (message) => {
    if (message.type() === 'error') errors.push(`console ${username}: ${message.text()}`)
  })
  await page.goto(`${BASE}/login`)
  await page.getByPlaceholder('请输入用户名').fill(username)
  await page.getByPlaceholder('请输入登录密码').fill(password)
  await page.getByRole('button', { name: '登录平台' }).click()
  await page.waitForURL((url) => url.pathname === expectedPath, { timeout: 20000 })
  await page.waitForLoadState('networkidle')
  return { context, page }
}

{
  const { context, page } = await pageFor('admin', 'admin123', '/admin/dashboard')
  await page.waitForTimeout(1400)
  assert.equal(await page.locator('.chart-box canvas').count(), 4)
  await page.goto(`${BASE}/admin/equipment`)
  await page.waitForLoadState('networkidle')
  const total = (await page.locator('.el-pagination__total').innerText()).trim()
  assert.ok(total.includes('共'))
  await page.getByRole('button', { name: '新增设备' }).first().click()
  await page.locator('.el-dialog').waitFor({ state: 'visible' })
  await page.keyboard.press('Escape')
  console.log(`PASS 管理员看板/台账（锁定版） :: 4 图 / ${total}`)
  await context.close()
}

{
  const { context, page } = await pageFor('reporter1', '123456', '/reporter/home')
  await page.goto(`${BASE}/reporter/repair-new`)
  await page.waitForLoadState('networkidle')
  await page.locator('.el-select').first().click()
  await page.locator('.el-select-dropdown__item').filter({ hasText: 'LAB-PC-001' }).first().waitFor({ state: 'visible' })
  await page.keyboard.press('Escape')
  console.log('PASS 报修者设备下拉（锁定版）')
  await context.close()
}

{
  const { context, page } = await pageFor('repairer1', '123456', '/repairer/orders')
  await page.goto(`${BASE}/repairer/inspection`)
  await page.waitForLoadState('networkidle')
  await page.locator('.result-group').getByText('异常', { exact: true }).click()
  const checked = await page.locator('.result-group .el-radio-button.is-active').innerText()
  assert.equal(checked.trim(), '异常')
  await page.locator('.quick-points').getByRole('button', { name: 'P-001' }).click()
  assert.equal(await page.getByPlaceholder('例如 P-001').inputValue(), 'P-001')
  console.log('PASS 巡检结果与快速点位（锁定版）')
  await context.close()
}

await browser.close()
console.log('ERRORS:', errors.length ? errors.join('\n') : 'NONE')
assert.equal(errors.length, 0)
console.log('LOCKED_UI_SMOKE_PASSED')
