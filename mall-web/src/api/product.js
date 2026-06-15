import request from '@/utils/request'

// 分类
export function getCategoryTree() {
  return request.get('/product/category/tree')
}

// 品牌
export function getBrandPage(params) {
  return request.get('/product/brand/page', { params })
}

// SPU
export function getSpuPage(params) {
  return request.get('/product/spu/page', { params })
}
export function getSpuDetail(id) {
  return request.get(`/product/spu/${id}`)
}
export function createSpu(data) {
  return request.post('/product/spu', data)
}
export function updateSpu(id, data) {
  return request.put(`/product/spu/${id}`, data)
}
export function deleteSpu(id) {
  return request.delete(`/product/spu/${id}`)
}
export function publishSpu(id, status) {
  return request.put(`/product/spu/${id}/publish`, null, { params: { status } })
}

// SKU
export function getSkuList(spuId) {
  return request.get(`/product/sku/list/${spuId}`)
}
export function createSku(data) {
  return request.post('/product/sku', data)
}
export function updateSku(id, data) {
  return request.put(`/product/sku/${id}`, data)
}
export function deleteSku(id) {
  return request.delete(`/product/sku/${id}`)
}
export function batchSkuPrice(data) {
  return request.put('/product/sku/price', data)
}
export function updateSkuStock(skuId, stock) {
  return request.put(`/product/sku/stock/${skuId}`, null, { params: { stock } })
}
