import { mkdirSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'

const outputDir = join(process.cwd(), 'screenshots', 'mobile-acceptance')
const widths = [320, 375, 390, 430]
const routes = [['catalog', '/'], ['cart', '/cart'], ['checkout', '/checkout'], ['orders', '/orders'], ['order-detail', '/orders/CBEC202609060000000001238899'], ['account', '/account']]
const root = 'http://127.0.0.1:5174'
const api = data => ({ code: 200, data })
const mock = url => {
  const path = new URL(url).pathname
  if (path === '/api/store/products') return api({ records: [{ id: 1, spuName: '北欧深海冷压鱼油软胶囊 1000mg 进口营养补充装', originCountry: '挪威', salesCount: 128 }] })
  if (path === '/api/store/products/1') return api({ spu: { spuName: '北欧深海冷压鱼油软胶囊' }, skus: [{ id: 11, skuCode: 'NORWAY-FISH-OIL-1000MG-120CAPS', currency: 'CNY', price: '199.00' }] })
  if (path === '/api/trade/cart') return api([{ id: 9, skuId: 'NORWAY-FISH-OIL-1000MG-120CAPS', quantity: 2, checked: 1 }])
  if (path === '/api/member/auth/profile') return api({ nickName: '海路会员', phone: '13800138000' })
  if (path === '/api/member/address') return api([{ id: 3, receiverName: '张晓明', receiverPhone: '13800138000', isDefault: 1, province: '吉林省', city: '四平市', district: '铁东区', detailAddress: '解放路跨境电商服务中心 1008 室' }])
  if (path === '/api/member/favorite/list') return api({ records: [{ id: 1 }] })
  if (path === '/api/member/browse/list') return api({ records: [{ id: 1 }, { id: 2 }] })
  if (path === '/api/trade/order/list') return api({ records: [{ orderNo: 'CBEC202609060000000001238899', payAmount: 'CNY 398.00', orderStatus: 2, createTime: '2026-09-06 09:20:00' }] })
  if (path === '/api/trade/order/CBEC202609060000000001238899') return api({ orderNo: 'CBEC202609060000000001238899', orderStatus: 2, payAmount: 'CNY 398.00', totalAmount: 'CNY 398.00', discountAmount: '0.00', freightAmount: '0.00', receiverName: '张晓明', receiverPhone: '13800138000', receiverAddress: '吉林省四平市铁东区解放路 1008 室', createTime: '2026-09-06 09:20:00' })
  if (path === '/api/trade/logistics/CBEC202609060000000001238899') return api({ logisticsCompany: '国际快递', logisticsNo: 'INTL20260906001' })
  if (path === '/api/trade/settle/check') return api({ payAmount: 'CNY 398.00' })
  return api({})
}

const target = await (await fetch('http://127.0.0.1:9223/json/new', { method: 'PUT' })).json()
const socket = new WebSocket(target.webSocketDebuggerUrl)
const waiting = new Map()
let sequence = 0
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms))
const command = (method, params = {}) => new Promise((resolve, reject) => {
  const id = ++sequence
  waiting.set(id, { resolve, reject })
  socket.send(JSON.stringify({ id, method, params }))
})

socket.onmessage = async ({ data }) => {
  const message = JSON.parse(data)
  if (message.id) {
    const task = waiting.get(message.id)
    waiting.delete(message.id)
    message.error ? task.reject(message.error) : task.resolve(message.result)
    return
  }
  if (message.method !== 'Fetch.requestPaused') return
  const { requestId, request } = message.params
  const body = mock(request.url)
  if (new URL(request.url).pathname.startsWith('/api/')) {
    await command('Fetch.fulfillRequest', { requestId, responseCode: 200, responseHeaders: [{ name: 'Content-Type', value: 'application/json' }], body: Buffer.from(JSON.stringify(body)).toString('base64') })
  } else await command('Fetch.continueRequest', { requestId })
}
await new Promise(resolve => { socket.onopen = resolve })
await command('Page.enable')
await command('Fetch.enable', { patterns: [{ urlPattern: '*', requestStage: 'Request' }] })
await command('Page.addScriptToEvaluateOnNewDocument', { source: "localStorage.setItem('member-token', 'mobile-audit-token')" })
mkdirSync(outputDir, { recursive: true })
const results = []
for (const width of widths) {
  await command('Emulation.setDeviceMetricsOverride', { width, height: 900, deviceScaleFactor: 1, mobile: true })
  for (const [name, route] of routes) {
    await command('Page.navigate', { url: `${root}${route}` })
    await sleep(900)
    await command('Runtime.evaluate', { expression: 'window.scrollTo(0, 0)' })
    await sleep(100)
    const metrics = await command('Runtime.evaluate', { expression: "JSON.stringify({scrollWidth:document.documentElement.scrollWidth,clientWidth:document.documentElement.clientWidth,buttons:[...document.querySelectorAll('button')].map(button=>{const box=button.getBoundingClientRect();return {text:button.innerText.trim(),width:Math.round(box.width),height:Math.round(box.height)}})})", returnByValue: true })
    const image = await command('Page.captureScreenshot', { format: 'png', captureBeyondViewport: true, clip: { x: 0, y: 0, width, height: 900, scale: 1 } })
    writeFileSync(join(outputDir, `${width}-${name}.png`), Buffer.from(image.data, 'base64'))
    results.push({ width, page: name, ...JSON.parse(metrics.result.value) })
  }
}
writeFileSync(join(outputDir, 'metrics.json'), JSON.stringify(results, null, 2))
console.log(JSON.stringify(results, null, 2))
socket.close()
