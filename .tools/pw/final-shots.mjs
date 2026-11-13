import { chromium } from 'playwright-core'

const browser = await chromium.launch({
  executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe',
  headless: true,
  args: ['--disable-gpu', '--no-first-run']
})
const context = await browser.newContext({ viewport: { width: 1440, height: 1000 }, deviceScaleFactor: 1 })
const page = await context.newPage()

await page.goto('http://localhost:5173/login')
await page.waitForLoadState('networkidle')
await page.waitForTimeout(500)
await page.screenshot({ path: 'D:/zhongyinbei/.tools/screens/final-login.png', fullPage: true })

await page.getByPlaceholder('请输入用户名').fill('admin')
await page.getByPlaceholder('请输入登录密码').fill('admin123')
await page.getByRole('button', { name: '登录平台' }).click()
await page.waitForURL((url) => url.pathname === '/admin/dashboard')
await page.waitForTimeout(4500)
await page.screenshot({ path: 'D:/zhongyinbei/.tools/screens/final-dashboard.png', fullPage: true })

await browser.close()
console.log('FINAL_SCREENSHOTS_DONE')
