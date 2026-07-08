import { ref } from 'vue'
import { message } from 'ant-design-vue'

/**
 * 通用 CRUD 弹窗 composable
 *
 * @param {object} options
 * @param {function} options.createApi — 新增 API 函数
 * @param {function} options.updateApi — 更新 API 函数
 * @param {function} options.deleteApi — 删除 API 函数
 * @param {function} options.fetchList  — 刷新列表函数
 * @param {object} options.defaultForm    — 表单默认值
 * @param {object} options.labels         — 操作提示文案
 * @returns 一组用于 CRUD 状态管理的方法
 */
export function useCrudModal({ createApi, updateApi, deleteApi, fetchList, defaultForm = {}, labels = {} }) {
  const modalOpen = ref(false)
  const saving = ref(false)
  const editingId = ref(null)
  const form = ref({ ...defaultForm })

  const msg = {
    created: '已创建',
    updated: '已更新',
    deleted: '已删除',
    ...labels
  }

  function resetForm() {
    form.value = { ...defaultForm }
    editingId.value = null
  }

  function openAdd() {
    resetForm()
    modalOpen.value = true
  }

  function openEdit(record) {
    editingId.value = record.id
    form.value = Object.keys(defaultForm).reduce((acc, key) => {
      acc[key] = record[key] !== undefined ? record[key] : defaultForm[key]
      return acc
    }, {})
    modalOpen.value = true
  }

  /**
   * 保存（新增或更新），返回 payload transformer 可自定义映射
   * @param {function} [payloadFn] — 可选的 payload 转换函数
   */
  async function handleSave(payloadFn) {
    saving.value = true
    try {
      const payload = payloadFn ? payloadFn(form.value) : { ...form.value }
      if (editingId.value) {
        await updateApi(editingId.value, payload)
        message.success(msg.updated)
      } else {
        await createApi(payload)
        message.success(msg.created)
      }
      modalOpen.value = false
      fetchList()
    } catch { /* ignore */ } finally {
      saving.value = false
    }
  }

  async function handleDelete(id) {
    await deleteApi(id)
    message.success(msg.deleted)
    fetchList()
  }

  return {
    modalOpen, saving, editingId, form,
    openAdd, openEdit, handleSave, handleDelete, resetForm
  }
}
