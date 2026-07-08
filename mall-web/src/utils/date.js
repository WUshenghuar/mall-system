/**
 * 通用日期格式化工具 — 原生 Intl.DateTimeFormat 实现，零依赖
 *
 * 用法：
 *   import { formatDate, formatDateTime } from '@/utils/date'
 *   formatDateTime(record.createTime)  // "2026-07-07 14:30"
 *   formatDate(record.createTime)     // "2026-07-07"
 */

const dateTimeFmt = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit'
})

const dateFmt = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit'
})

function toDate(v) {
  if (!v) return null
  if (v instanceof Date) return v
  const d = new Date(v)
  return isNaN(d.getTime()) ? null : d
}

/** 格式化日期时间：YYYY-MM-DD HH:mm */
export function formatDateTime(v) {
  const d = toDate(v)
  if (!d) return '--'
  return dateTimeFmt.format(d).replace(/\//g, '-')
}

/** 格式化日期：YYYY-MM-DD */
export function formatDate(v) {
  const d = toDate(v)
  if (!d) return '--'
  return dateFmt.format(d).replace(/\//g, '-')
}
