import { chromium } from 'playwright-core'

const browser = await chromium.launch({
  executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe',
  headless: true
})
const context = await browser.newContext({ viewport: { width: 390, height: 844 } })
const page = await context.newPage()
await page.goto('http://localhost:5173/login')
await page.getByPlaceholder('请输入用户名').fill('reporter1')
await page.getByPlaceholder('请输入登录密码').fill('123456')
await page.getByRole('button', { name: '登录平台' }).click()
await page.waitForURL((url) => url.pathname === '/reporter/home')
await page.waitForTimeout(1000)
const offenders = await page.evaluate(() => {
  const width = window.innerWidth
  return [...document.querySelectorAll('*')]
    .map((el) => {
      const r = el.getBoundingClientRect()
      return {
        tag: el.tagName,
        cls: String(el.className || '').slice(0, 110),
        left: Math.round(r.left),
        right: Math.round(r.right),
        width: Math.round(r.width)
      }
    })
    .filter((item) => item.width > 0 && item.right > width + 1)
    .slice(0, 30)
})
console.log(JSON.stringify(offenders, null, 2))
await browser.close()
