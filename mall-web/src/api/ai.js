import request from '@/utils/request'

export function getSupportTicketPage(params) {
  return request.get('/ai/tickets/page', { params })
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
