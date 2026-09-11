import request from '@/utils/request'

export function getSupportTicketPage(params) {
  return request.get('/ai/tickets/page', { params })
}

export function claimSupportTicket(id) {
  return request.post(`/ai/tickets/${id}/claim`)
}

export function resolveSupportTicket(id, note) {
  return request.post(`/ai/tickets/${id}/resolve`, { note })
}
