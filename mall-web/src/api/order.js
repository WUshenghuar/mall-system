import request from '@/utils/request'

export function getOrderPage(params) {
  return request.get('/order/page', { params })
}

export function getOrderDetail(id) {
  return request.get(`/order/${id}`)
}

export function createOrder(data) {
  return request.post('/order/create', data)
}

export function payOrder(id) {
  return request.post(`/order/${id}/pay`)
}

export function cancelOrder(id) {
  return request.post(`/order/${id}/cancel`)
}

// 退款
export function getRefundPage(params) {
  return request.get('/order/refund/page', { params })
}

export function getRefundDetail(id) {
  return request.get(`/order/refund/${id}`)
}

export function applyRefund(data) {
  return request.post('/order/refund', data)
}

export function approveRefund(id, comment) {
  return request.put(`/order/refund/${id}/approve`, null, { params: { comment } })
}

export function rejectRefund(id, comment) {
  return request.put(`/order/refund/${id}/reject`, null, { params: { comment } })
}
