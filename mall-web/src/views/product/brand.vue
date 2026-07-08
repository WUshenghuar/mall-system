<template>
  <div class="sub-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">品牌管理</h2>
        <p class="page-desc">维护商品品牌信息</p>
      </div>
      <div class="head-actions">
        <a-space>
          <a-input-search v-model:value="keyword" placeholder="搜索品牌" style="width:200px"
            @search="handleSearch" allow-clear />
          <a-button type="primary" @click="openAdd">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:4px;vertical-align:middle"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            新增品牌
          </a-button>
        </a-space>
      </div>
    </div>

    <a-card :bordered="false">
      <a-table :columns="columns" :data-source="list" :loading="loading"
        row-key="id" size="middle" :pagination="pagination" @change="onTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'default'">
              {{ record.status === 1 ? '启用' : '停用' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button size="small" type="link" @click="openEdit(record)">编辑</a-button>
              <a-popconfirm title="确定删除？" @confirm="handleDelete(record.id)">
                <a-button size="small" type="link" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="modalOpen" :title="editId ? '编辑品牌' : '新增品牌'"
      @ok="handleSave" :confirm-loading="saving" destroy-on-close>
      <a-form layout="vertical" style="margin-top:12px">
        <a-form-item label="品牌名称" required>
          <a-input v-model:value="form.brandName" placeholder="请输入品牌名称" />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="form.orderNum" :min="0" style="width:100%" />
        </a-form-item>
        <a-form-item label="状态">
          <a-switch v-model:checked="form.status" :checked-value="1" :unchecked-value="0"
            checked-children="启用" un-checked-children="停用" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getBrandPage, createBrand, updateBrand, deleteBrand } from '@/api/product'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const modalOpen = ref(false)
const editId = ref(null)
const keyword = ref('')
const form = ref({ brandName: '', orderNum: 0, status: 1 })

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: t => `共 ${t} 条`
})

const columns = [
  { title: '品牌名称', dataIndex: 'brandName' },
  { title: '排序', dataIndex: 'orderNum', width: 80 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 150 }
]

async function fetchData() {
  loading.value = true
  try {
    const res = await getBrandPage({
      page: pagination.current,
      size: pagination.pageSize,
      keyword: keyword.value || undefined
    })
    const page = res.data
    list.value = page.records || []
    pagination.total = page.total || 0
  } finally { loading.value = false }
}

function onTableChange(pag) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function openAdd() {
  editId.value = null
  form.value = { brandName: '', orderNum: 0, status: 1 }
  modalOpen.value = true
}

function openEdit(record) {
  editId.value = record.id
  form.value = { brandName: record.brandName || '', orderNum: record.orderNum ?? 0, status: record.status ?? 1 }
  modalOpen.value = true
}

async function handleSave() {
  if (!form.value.brandName) { message.warning('请填写品牌名称'); return }
  saving.value = true
  try {
    if (editId.value) {
      await updateBrand(editId.value, form.value)
      message.success('已更新')
    } else {
      await createBrand(form.value)
      message.success('已创建')
    }
    modalOpen.value = false
    fetchData()
  } catch { /* ignore */ } finally { saving.value = false }
}

async function handleDelete(id) {
  try {
    await deleteBrand(id)
    message.success('已删除')
    fetchData()
  } catch { /* ignore */ }
}

onMounted(fetchData)
</script>
