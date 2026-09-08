import request from '@/utils/request'

export function getDashboardStats(range = 'today') {
  return request.get('/dashboard/stats', { params: { range } })
}
