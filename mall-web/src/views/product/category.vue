<template>
  <div class="sub-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">分类管理</h2>
        <p class="page-desc">维护商品分类树</p>
      </div>
      <div class="head-actions">
        <a-button type="primary" @click="openAdd(null)">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:4px;vertical-align:middle"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          新增分类
        </a-button>
      </div>
    </div>

    <a-card :bordered="false">
      <a-table :columns="columns" :data-source="list" :loading="loading"
        row-key="id" size="middle" :pagination="false"
        :expandable="{ defaultExpandAllRows: true }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'default'">
              {{ record.status === 1 ? '启用' : '停用' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button size="small" type="link" @click="openAdd(record)">添加子级</a-button>
              <a-button size="small" type="link" @click="openEdit(record)">编辑</a-button>
              <a-popconfirm title="确定删除？" @confirm="handleDelete(record.id)">
                <a-button size="small" type="link" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="modalOpen" :title="editId ? '编辑分类' : '新增分类'" @ok="handleSave" @cancel="modalOpen=false">
      <a-form layout="vertical">
        <a-form-item label="分类名称">
          <a-input v-model:value="form.categoryName" placeholder="请输入分类名称" />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="form.orderNum" :min="0" style="width:100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getCategoryTree, createCategory, updateCategory, deleteCategory } from '@/api/product'

const loading = ref(false)
const list = ref([])
const modalOpen = ref(false)
const editId = ref(null)
const form = ref({ categoryName: '', parentId: 0, orderNum: 0, status: 1 })

const columns = [
  { title: '名称', dataIndex: 'categoryName', key: 'name' },
  { title: '排序', dataIndex: 'orderNum', key: 'sort', width: 80 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 200 }
]

async function fetchData() {
  loading.value = true
  try {
    const res = await getCategoryTree()
    list.value = Array.isArray(res.data) ? res.data : []
  } finally { loading.value = false }
}

function openAdd(parent) {
  editId.value = null
  form.value = { categoryName: '', parentId: parent?.id || 0, orderNum: 0, status: 1 }
  modalOpen.value = true
}

function openEdit(record) {
  editId.value = record.id
  form.value = { ...record }
  modalOpen.value = true
}

async function handleSave() {
  try {
    if (editId.value) {
      await updateCategory(editId.value, form.value)
    } else {
      await createCategory(form.value)
    }
    modalOpen.value = false
    fetchData()
  } catch { /* handled by request interceptor */ }
}

async function handleDelete(id) {
  await deleteCategory(id)
  fetchData()
}

onMounted(fetchData)
</script>
