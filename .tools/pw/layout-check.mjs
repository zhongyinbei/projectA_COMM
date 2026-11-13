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

async function login(page, username, password, expectedPath) {
  await page.goto(`${BASE}/login`)
  await page.getByPlaceholder('请输入用户名').fill(username)
  await page.getByPlaceholder('请输入登录密码').fill(password)
  await page.getByRole('button', { name: '登录平台' }).click()
  await page.waitForURL((url) => url.pathname === expectedPath, { timeout: 20000 })
  await page.waitForLoadState('networkidle')
}

{
  const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
  const page = await context.newPage()
  await login(page, 'admin', 'admin123', '/admin/dashboard')
  await page.waitForTimeout(1200)
  const topbar = (await page.locator('.topbar h1').innerText()).trim()
  assert.equal(topbar, '运营管理中心')
  await page.goto(`${BASE}/admin/equipment`)
  await page.waitForLoadState('networkidle')
  const totalText = (await page.locator('.el-pagination__total').innerText()).trim()
  assert.ok(totalText.includes('共'), `分页文案未中文化: ${totalText}`)
  const pageSizeText = (await page.locator('.el-pagination .el-select__placeholder').first().innerText()).trim()
  await page.screenshot({ path: `${SHOTS}/admin-equipment-final.png`, fullPage: true })
  console.log(`PASS 中文分页文案 :: ${totalText} / ${pageSizeText}`)
  await context.close()
}

{
  const context = await browser.newContext({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 1 })
  const page = await context.newPage()
  await login(page, 'reporter1', '123456', '/reporter/home')
  await page.waitForLoadState('networkidle')
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth)
  await page.screenshot({ path: `${SHOTS}/mobile-reporter-home.png`, fullPage: true })
  console.log(`INFO 移动端报修者横向溢出 ${overflow}px`)
  await context.close()
}

{
  const context = await browser.newContext({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 1 })
  const page = await context.newPage()
  await login(page, 'admin', 'admin123', '/admin/dashboard')
  await page.waitForTimeout(1500)
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth)
  await page.screenshot({ path: `${SHOTS}/mobile-admin-dashboard.png`, fullPage: true })
  console.log(`INFO 移动端管理员横向溢出 ${overflow}px`)
  await context.close()
}

await browser.close()
console.log('LAYOUT_CHECKS_DONE')
