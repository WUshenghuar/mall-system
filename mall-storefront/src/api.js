import axios from 'axios'

const request = axios.create({ baseURL: '/api', timeout: 10000 })
request.interceptors.request.use(config => {
  const token = localStorage.getItem('member-token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})
request.interceptors.response.use(response => response.data, error => Promise.reject(error.response?.data?.message || '网络请求失败'))

export const storeApi = {
  products: params => request.get('/store/products', { params }),
  detail: id => request.get(`/store/products/${id}`),
  categories: () => request.get('/store/categories')
}
export const memberApi = {
  login: data => request.post('/member/auth/login', data),
  register: data => request.post('/member/auth/register', data),
  profile: () => request.get('/member/auth/profile'),
  addresses: () => request.get('/member/address'),
  addAddress: data => request.post('/member/address', data),
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
  pay: data => request.post('/trade/pay/create', data),
  simulate: payNo => request.post(`/trade/pay/${payNo}/simulate-success`)
}
