import request from '@/utils/request'

export function getMemberPage(params) {
  return request.get('/member/page', { params })
}

export function getMemberDetail(id) {
  return request.get(`/member/${id}`)
}

export function adjustPoints(id, points, reason) {
  return request.post(`/member/${id}/points`, null, { params: { points, reason } })
}
