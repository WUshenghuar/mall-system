import { createServer } from 'node:http'

const ok = data => JSON.stringify({ code: 200, data })
const products = [
  { id: 1, spuName: '北欧深海冷压鱼油软胶囊 1000mg 进口营养补充装', originCountry: '挪威', salesCount: 128 },
  { id: 2, spuName: '瑞士原产有机坚果燕麦早餐谷物 750g', originCountry: '瑞士', salesCount: 96 },
  { id: 3, spuName: '德国百年药妆修护面霜 50ml 敏感肌适用', originCountry: '德国', salesCount: 210 },
  { id: 4, spuName: '法国波尔多 AOC 干红葡萄酒 2019 年份礼盒', originCountry: '法国', salesCount: 64 },
  { id: 5, spuName: '日本匠人手作陶瓷马克杯 云纹釉 350ml', originCountry: '日本', salesCount: 43 },
  { id: 6, spuName: '新西兰麦卢卡蜂蜜 UMF10+ 250g 铁罐装', originCountry: '新西兰', salesCount: 175 },
  { id: 7, spuName: '意大利头层牛皮商务双肩包 15.6 英寸', originCountry: '意大利', salesCount: 31 },
  { id: 8, spuName: '澳大利亚进口全脂奶粉 1kg 家庭装', originCountry: '澳大利亚', salesCount: 258 }
]
const skus = [
  { id: 11, skuCode: 'NORWAY-FISH-OIL-1000MG-120CAPS', currency: 'CNY', price: '199.00' },
  { id: 12, skuCode: 'SWISS-GRANOLA-750G-OAT', currency: 'CNY', price: '89.00' },
  { id: 13, skuCode: 'DE-CREAM-50ML-SENSITIVE', currency: 'CNY', price: '259.00' },
  { id: 14, skuCode: 'FR-BORDEAUX-2019-750ML', currency: 'CNY', price: '329.00' },
  { id: 15, skuCode: 'JP-MUG-CLOUD-350ML', currency: 'CNY', price: '129.00' },
  { id: 16, skuCode: 'NZ-MANUKA-UMF10-250G', currency: 'CNY', price: '459.00' },
  { id: 17, skuCode: 'IT-BACKPACK-15INCH-LEATHER', currency: 'CNY', price: '1299.00' },
  { id: 18, skuCode: 'AU-MILK-POWDER-1KG', currency: 'CNY', price: '119.00' }
]
const orderNo = 'CBEC202609060000000001238899'
const cart = [
  { id: 9, skuId: 'NORWAY-FISH-OIL-1000MG-120CAPS', quantity: 2, checked: 1 },
  { id: 10, skuId: 'NZ-MANUKA-UMF10-250G', quantity: 1, checked: 1 }
]
const order = { orderNo, orderStatus: 2, payAmount: 'CNY 398.00', totalAmount: 'CNY 398.00', discountAmount: '0.00', freightAmount: '0.00', receiverName: '张晓明', receiverPhone: '13800138000', receiverAddress: '吉林省四平市铁东区解放路跨境电商服务中心 1008 室', createTime: '2026-09-06 09:20:00' }
const address = [{ id: 3, receiverName: '张晓明', receiverPhone: '13800138000', isDefault: 1, province: '吉林省', city: '四平市', district: '铁东区', detailAddress: '解放路跨境电商服务中心 1008 室' }]

createServer((req, res) => {
  const { pathname } = new URL(req.url, 'http://localhost')
  res.setHeader('Content-Type', 'application/json; charset=utf-8')
  if (req.url.includes('/api/store/products/')) { res.end(ok({ spu: { spuName: products[Number(pathname.split('/').pop()) - 1]?.spuName || '测试商品' }, skus: skus.slice(0, 2) })); return }
  switch (pathname) {
    case '/api/store/products': res.end(ok({ records: products })); return
    case '/api/trade/cart': res.end(ok(cart)); return
    case '/api/trade/settle/check': res.end(ok({ payAmount: 'CNY 487.00' })); return
    case '/api/member/auth/profile': res.end(ok({ nickName: '海路会员', phone: '13800138000' })); return
    case '/api/member/address': res.end(ok(address)); return
    case '/api/member/favorite/list': res.end(ok({ records: [{ id: 1, spuId: 1 }, { id: 2, spuId: 4 }] })); return
    case '/api/member/browse/list': res.end(ok({ records: [{ id: 1, spuId: 2 }, { id: 2, spuId: 5 }] })); return
    case '/api/trade/order/list': res.end(ok({ records: [order] })); return
    case '/api/trade/refund': res.end(ok({ records: [] })); return
    default:
      if (pathname.startsWith('/api/trade/order/') && pathname.includes('/cancel')) { res.end(ok(null)); return }
      if (pathname.startsWith('/api/trade/order/') && pathname.includes('/confirm')) { res.end(ok(null)); return }
      if (pathname.startsWith('/api/trade/logistics/')) { res.end(ok({ logisticsCompany: '国际快递', logisticsNo: 'INTL20260906001' })); return }
      if (pathname.startsWith('/api/trade/order/')) { res.end(ok(order)); return }
      if (pathname.startsWith('/api/member/address/') && req.method === 'DELETE') { res.end(ok(null)); return }
      if (pathname.startsWith('/api/member/address/') && req.method === 'PUT') { res.end(ok(null)); return }
      if (pathname.startsWith('/api/trade/cart/')) { res.end(ok(null)); return }
      res.end(ok({}))
  }
}).listen(18100, () => console.log('[mock] mall API mock 已启动于 http://127.0.0.1:18100'))
