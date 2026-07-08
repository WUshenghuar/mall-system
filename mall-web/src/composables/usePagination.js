import { ref, reactive } from 'vue'

/**
 * 通用分页 composable
 * 封装分页状态、表格 change 处理、重置逻辑
 *
 * @param {object} options — { pageSize?: number, showSizeChanger?: boolean }
 * @returns {{ pagination, onTableChange, resetPage }}
 */
export function usePagination(options = {}) {
  const { pageSize = 10, showSizeChanger = true } = options

  const pagination = reactive({
    current: 1,
    pageSize,
    total: 0,
    showSizeChanger,
    showTotal: t => `共 ${t} 条`
  })

  function onTableChange(pag) {
    pagination.current = pag.current
    pagination.pageSize = pag.pageSize
  }

  function resetPage() {
    pagination.current = 1
  }

  return { pagination, onTableChange, resetPage }
}
