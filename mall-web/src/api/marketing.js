import request from '@/utils/request'

// 优惠券
export function getCouponPage(params) {
  return request.get('/marketing/coupon/page', { params })
}
export function getCouponDetail(id) {
  return request.get(`/marketing/coupon/${id}`)
}
export function createCoupon(data) {
  return request.post('/marketing/coupon', data)
}
export function updateCoupon(id, data) {
  return request.put(`/marketing/coupon/${id}`, data)
}
export function deleteCoupon(id) {
  return request.delete(`/marketing/coupon/${id}`)
}
export function submitAudit(id) {
  return request.post(`/marketing/coupon/${id}/submit-audit`)
}
export function auditCoupon(id, status, comment) {
  return request.put(`/marketing/coupon/${id}/audit`, null, { params: { status, comment } })
}

// 活动
export function getActivityPage(params) {
  return request.get('/marketing/activity/page', { params })
}
export function createActivity(data) {
  return request.post('/marketing/activity', data)
}
export function updateActivity(id, data) {
  return request.put(`/marketing/activity/${id}`, data)
}
export function deleteActivity(id) {
  return request.delete(`/marketing/activity/${id}`)
}
export function prepareSeckill(id, skuId, totalStock) {
  return request.post(`/marketing/activity/${id}/seckill/prepare`, null, { params: { skuId, totalStock } })
}
