import request from '@/utils/request'

export function getAiKnowledge() {
  return request.get('/ai/knowledge')
}

export function saveAiKnowledge(data) {
  return request.post('/ai/knowledge', data)
}

export function getSupportTicketPage(params) {
  return request.get('/ai/tickets/page', { params })
}

export function getAiFeedbackStats() {
  return request.get('/ai/feedback/stats')
}

export function getAiAuditPage(params) {
  return request.get('/ai/audit', { params })
}

export function getSupportTicketConversation(id) {
  return request.get(`/ai/tickets/${id}/conversation`)
}

export function claimSupportTicket(id) {
  return request.post(`/ai/tickets/${id}/claim`)
}

export function replySupportTicket(id, message) {
  return request.post(`/ai/tickets/${id}/reply`, { message })
}

export function resolveSupportTicket(id, note) {
  return request.post(`/ai/tickets/${id}/resolve`, { note })
}
