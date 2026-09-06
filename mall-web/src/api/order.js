import request from '@/utils/request'

export function getOrderPage(params) {
  return request.get('/order/page', { params })
}

export function getTradeOrderPage(params) {
  return request.get('/order/trade/page', { params })
}

export function shipTradeOrder(orderNo, data) {
  return request.post(`/order/trade/${orderNo}/ship`, data)
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
  return request.get('/order/trade/refund', { params })
}

export function getRefundDetail(id) {
  return request.get(`/order/refund/${id}`)
}

export function applyRefund(data) {
  return request.post('/order/refund', data)
}

export function approveRefund(id, comment) {
  return request.post(`/order/trade/refund/${id}/approve`, { comment })
}

export function rejectRefund(id, comment) {
  return request.post(`/order/trade/refund/${id}/reject`, { comment })
}
export function completeRefund(id, comment) { return request.post(`/order/trade/refund/${id}/complete`, { comment }) }
