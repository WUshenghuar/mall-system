import axios from 'axios'

const request = axios.create({ baseURL: '/api', timeout: 10000 })
request.interceptors.request.use(config => {
  const token = localStorage.getItem('member-token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})
request.interceptors.response.use(response => {
  const body = response.data
  if (typeof body?.code === 'number' && body.code !== 200) {
    if ([401, 403].includes(body.code)) localStorage.removeItem('member-token')
    return Promise.reject(body.message || '请求处理失败')
  }
  return body
}, error => {
  if ([401, 403].includes(error.response?.status)) {
    localStorage.removeItem('member-token')
    return Promise.reject('登录状态失效，请重新登录')
  }
  return Promise.reject(error.response?.data?.message || error.message || '网络请求失败')
})

export const storeApi = {
  products: params => request.get('/store/products', { params }),
  detail: id => request.get(`/store/products/${id}`),
  categories: () => request.get('/store/categories')
}
export const marketingApi = {
  coupons: () => request.get('/store/coupons'),
  claimCoupon: couponId => request.post(`/store/coupons/${couponId}/claim`),
  memberCoupons: () => request.get('/store/member/coupons'),
  activities: () => request.get('/store/activities')
}
export const aiApi = {
  async streamChat(payload, onEvent) {
    const token = localStorage.getItem('member-token')
    const response = await fetch('/api/ai/chat', {
      method: 'POST', headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      body: JSON.stringify(payload)
    })
    if (!response.ok || !response.body) throw new Error('客服服务暂不可用')
    const reader = response.body.getReader(), decoder = new TextDecoder()
    let buffer = ''
    while (true) {
      const { done, value } = await reader.read()
      buffer += decoder.decode(value || new Uint8Array(), { stream: !done })
      const frames = buffer.split(/\r?\n\r?\n/)
      buffer = frames.pop() || ''
      for (const frame of frames) {
        const data = frame.split(/\r?\n/).find(line => line.startsWith('data:'))?.slice(5).trim()
        if (data) onEvent(JSON.parse(data))
      }
      if (done) break
    }
  },
  handoff: data => request.post('/ai/tickets', data)
}
export const memberApi = {
  login: data => request.post('/member/auth/login', data),
  register: data => request.post('/member/auth/register', data),
  profile: () => request.get('/member/auth/profile'),
  addresses: () => request.get('/member/address'),
  addAddress: data => request.post('/member/address', data),
  updateAddress: (id, data) => request.put(`/member/address/${id}`, data),
  removeAddress: id => request.delete(`/member/address/${id}`),
  setDefaultAddress: id => request.put(`/member/address/${id}/default`),
  favorites: params => request.get('/member/favorite/list', { params }),
  addFavorite: spuId => request.post('/member/favorite', { spuId }),
  removeFavorite: spuId => request.delete(`/member/favorite/${spuId}`),
  browseHistory: params => request.get('/member/browse/list', { params }),
  recordBrowse: spuId => request.post('/member/browse', { spuId })
}
export const tradeApi = {
  cart: () => request.get('/trade/cart'),
  addCart: data => request.post('/trade/cart', data),
  updateCart: (id, data) => request.put(`/trade/cart/${id}`, data),
  removeCart: id => request.delete(`/trade/cart/${id}`),
  settle: data => request.post('/trade/settle/check', data),
  createOrder: data => request.post('/trade/order', data),
  orders: params => request.get('/trade/order/list', { params }),
  order: orderNo => request.get(`/trade/order/${orderNo}`),
  cancelOrder: orderNo => request.post(`/trade/order/${orderNo}/cancel`),
  confirmOrder: orderNo => request.post(`/trade/order/${orderNo}/confirm`),
  logistics: orderNo => request.get(`/trade/logistics/${orderNo}`),
  refunds: params => request.get('/trade/refund', { params }),
  applyRefund: data => request.post('/trade/refund', data),
  pay: data => request.post('/trade/pay/create', data),
  simulate: payNo => request.post(`/trade/pay/${payNo}/simulate-success`)
}
