import request from '@/utils/request'

// 对账单
export function getStatementPage(params) {
  return request.get('/finance/statement/page', { params })
}
export function getStatementDetail(id) {
  return request.get(`/finance/statement/${id}`)
}
export function confirmStatement(id) {
  return request.put(`/finance/statement/${id}/confirm`)
}
export function exportStatement(id) {
  return request.get(`/finance/statement/${id}/export`, { responseType: 'blob' })
}

// 关税配置
export function getTaxPage(params) {
  return request.get('/finance/tax/page', { params })
}
export function createTax(data) {
  return request.post('/finance/tax', data)
}
export function updateTax(id, data) {
  return request.put(`/finance/tax/${id}`, data)
}
export function deleteTax(id) {
  return request.delete(`/finance/tax/${id}`)
}
