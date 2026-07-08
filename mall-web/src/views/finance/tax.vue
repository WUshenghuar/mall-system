<template>
  <div class="sub-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">关税税率配置</h2>
        <p class="page-desc">管理进口关税、增值税及消费税税率</p>
      </div>
      <div class="head-actions">
        <a-button type="primary" @click="openAdd">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:4px;vertical-align:middle"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          新增税率
        </a-button>
      </div>
    </div>

    <a-card :bordered="false">
      <a-table :columns="columns" :data-source="list" :loading="loading"
        row-key="id" size="middle" :pagination="pagination" @change="onPage">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'taxRate'">{{ record.taxRate }}%</template>
          <template v-if="column.key === 'effectiveDate'">
            {{ record.effectiveDate }} ~ {{ record.expireDate || '长期' }}
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

    <!-- Add / Edit Modal -->
    <a-modal v-model:open="modalOpen" :title="editingId ? '编辑税率' : '新增税率'"
      @ok="handleSave" :confirm-loading="saving" destroy-on-close>
      <a-form :model="form" layout="vertical" style="margin-top:16px">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="分类 ID" required>
              <a-input-number v-model:value="form.categoryId" :min="1" style="width:100%" placeholder="商品分类 ID" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="税率 (%)" required>
              <a-input-number v-model:value="form.taxRate" :min="0" :max="100" :precision="2" style="width:100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="原产国">
              <a-input v-model:value="form.originCountry" placeholder="如 CN" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="目的国">
              <a-input v-model:value="form.destCountry" placeholder="如 US" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="税种" required>
              <a-select v-model:value="form.taxType" :options="taxTypeOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="生效日期">
              <a-date-picker v-model:value="form.effectiveDate" style="width:100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="失效日期">
          <a-date-picker v-model:value="form.expireDate" style="width:100%" placeholder="留空表示长期有效" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getTaxPage, createTax, updateTax, deleteTax } from '@/api/finance'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const modalOpen = ref(false)
const editingId = ref(null)

const taxTypeOptions = [
  { label: '进口关税', value: 'IMPORT_DUTY' },
  { label: '增值税', value: 'VAT' },
  { label: '消费税', value: 'CONSUMPTION_TAX' },
  { label: '反倾销税', value: 'ANTI_DUMPING' }
]

const form = reactive({
  categoryId: null,
  taxRate: null,
  originCountry: '',
  destCountry: '',
  taxType: 'IMPORT_DUTY',
  effectiveDate: null,
  expireDate: null
})

const pagination = ref({ current: 1, pageSize: 10, total: 0 })

const columns = [
  { title: '分类 ID', dataIndex: 'categoryId' },
  { title: '原产国', dataIndex: 'originCountry' },
  { title: '目的国', dataIndex: 'destCountry' },
  { title: '税率', key: 'taxRate', width: 100 },
  { title: '税种', dataIndex: 'taxType', width: 120 },
  { title: '有效期', key: 'effectiveDate' },
  { title: '操作', key: 'action', width: 140 }
]

async function fetchData() {
  loading.value = true
  try {
    const res = await getTaxPage({ page: pagination.value.current, size: pagination.value.pageSize })
    list.value = res.data?.records || []
    pagination.value.total = res.data?.total || 0
  } finally { loading.value = false }
}

function onPage(p) { pagination.value.current = p.current; fetchData() }

function resetForm() {
  form.categoryId = null
  form.taxRate = null
  form.originCountry = ''
  form.destCountry = ''
  form.taxType = 'IMPORT_DUTY'
  form.effectiveDate = null
  form.expireDate = null
  editingId.value = null
}

function openAdd() { resetForm(); modalOpen.value = true }

function openEdit(r) {
  editingId.value = r.id
  form.categoryId = r.categoryId ?? null
  form.taxRate = r.taxRate ?? null
  form.originCountry = r.originCountry || ''
  form.destCountry = r.destCountry || ''
  form.taxType = r.taxType || 'IMPORT_DUTY'
  form.effectiveDate = r.effectiveDate || null
  form.expireDate = r.expireDate || null
  modalOpen.value = true
}

async function handleSave() {
  if (form.categoryId == null || form.taxRate == null) {
    message.warning('请填写分类 ID 和税率')
    return
  }
  saving.value = true
  try {
    const payload = { ...form }
    if (editingId.value) {
      await updateTax(editingId.value, payload)
      message.success('已更新')
    } else {
      await createTax(payload)
      message.success('已创建')
    }
    modalOpen.value = false
    fetchData()
  } catch { /* ignore */ } finally { saving.value = false }
}

async function handleDelete(id) {
  await deleteTax(id)
  message.success('已删除')
  fetchData()
}

onMounted(fetchData)
</script>
