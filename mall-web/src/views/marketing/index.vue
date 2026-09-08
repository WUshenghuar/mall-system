<template>
  <div class="sub-page">
    <!-- Page head -->
    <div class="page-head">
      <div>
        <h2 class="page-title">优惠券管理</h2>
        <p class="page-desc">创建、审核与发行优惠券</p>
      </div>
      <div class="head-actions">
        <a-button type="primary" @click="openAdd">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:4px;vertical-align:middle"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          新建优惠券
        </a-button>
      </div>
    </div>

    <a-card :bordered="false">

      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        @change="onTableChange"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">
            <a-tag v-if="record.couponType === 'FULL_REDUCTION'" color="blue">满减券</a-tag>
            <a-tag v-else-if="record.couponType === 'DISCOUNT'" color="purple">折扣券</a-tag>
            <a-tag v-else color="cyan">运费券</a-tag>
          </template>
          <template v-if="column.key === 'discount'">
            <span v-if="record.couponType === 'DISCOUNT'">{{ record.discount }}折</span>
            <span v-else>-${{ record.discount }}</span>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag v-if="record.status === 0">草稿</a-tag>
            <a-tag v-else-if="record.status === 1" color="orange">待审核</a-tag>
            <a-tag v-else-if="record.status === 2" color="green">已发布</a-tag>
            <a-tag v-else color="default">已结束</a-tag>
          </template>
          <template v-if="column.key === 'issued'">
            {{ record.issuedCount || 0 }} / {{ record.maxIssue || 0 }}
          </template>
          <template v-if="column.key === 'action'">
            <a-space size="small">
              <a-button v-if="record.status === 0 || record.status === 3" type="link" size="small" @click="openEdit(record)">
                编辑
              </a-button>
              <a-button v-if="record.status === 0" type="link" size="small" @click="handleSubmitAudit(record.id)">
                提交审核
              </a-button>
              <a-button v-if="record.status === 1" type="link" size="small" @click="handleAudit(record.id, 2)">
                通过
              </a-button>
              <a-button v-if="record.status === 1" type="link" size="small" danger @click="handleAudit(record.id, 3)">
                驳回
              </a-button>
              <a-popconfirm title="确定删除？" @confirm="handleDelete(record.id)">
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- Add / Edit Modal -->
    <a-modal v-model:open="modalOpen" :title="editingId ? '编辑优惠券' : '新建优惠券'"
      @ok="handleSave" :confirm-loading="saving" destroy-on-close width="520px">
      <a-form :model="form" layout="vertical" style="margin-top:16px">
        <a-form-item label="优惠券名称" required>
          <a-input v-model:value="form.couponName" placeholder="如 新用户满100减20" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="类型" required>
              <a-select v-model:value="form.couponType" :options="typeOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="使用范围">
              <a-select v-model:value="form.scope" :options="scopeOptions" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="门槛 ($)" required>
              <a-input-number v-model:value="form.threshold" :min="0" :precision="2" style="width:100%" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item :label="form.couponType === 'DISCOUNT' ? '折扣 (折)' : '减免金额'" required>
              <a-input-number v-model:value="form.discount" :min="0" :precision="2" style="width:100%" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="发行量" required>
              <a-input-number v-model:value="form.maxIssue" :min="1" style="width:100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="每人限领">
              <a-input-number v-model:value="form.perLimit" :min="1" style="width:100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="有效期" required>
              <a-range-picker v-model:value="form.validRange" show-time format="YYYY-MM-DD HH:mm" style="width:100%" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { getCouponPage, createCoupon, updateCoupon, deleteCoupon, submitAudit, auditCoupon } from '@/api/marketing'

const loading = ref(false)
const saving = ref(false)
const dataSource = ref([])
const modalOpen = ref(false)
const editingId = ref(null)

const typeOptions = [
  { label: '满减券', value: 'FULL_REDUCTION' },
  { label: '折扣券', value: 'DISCOUNT' },
  { label: '运费券', value: 'SHIPPING' }
]
const scopeOptions = [
  { label: '全平台', value: 'ALL' },
  { label: '指定分类', value: 'CATEGORY' },
  { label: '指定商品', value: 'SKU' }
]

const form = reactive({
  couponName: '',
  couponType: 'FULL_REDUCTION',
  scope: 'ALL',
  threshold: null,
  discount: null,
  maxIssue: 100,
  perLimit: 1,
  validRange: [dayjs(), dayjs().add(30, 'day')]
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: t => `共 ${t} 条`
})

const columns = [
  { title: '优惠券名称', dataIndex: 'couponName' },
  { title: '类型', key: 'type', width: 80 },
  { title: '门槛', dataIndex: 'threshold', width: 80 },
  { title: '减免', key: 'discount', width: 80 },
  { title: '发行/总量', key: 'issued', width: 90 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 260 }
]

async function fetchData() {
  loading.value = true
  try {
    const res = await getCouponPage({ page: pagination.current, size: pagination.pageSize })
    dataSource.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch { /* ignore */ } finally { loading.value = false }
}

function onTableChange(pag) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

function resetForm() {
  form.couponName = ''
  form.couponType = 'FULL_REDUCTION'
  form.scope = 'ALL'
  form.threshold = null
  form.discount = null
  form.maxIssue = 100
  form.perLimit = 1
  form.validRange = [dayjs(), dayjs().add(30, 'day')]
  editingId.value = null
}

function openAdd() {
  resetForm()
  modalOpen.value = true
}

function openEdit(r) {
  editingId.value = r.id
  form.couponName = r.couponName || ''
  form.couponType = r.couponType || 'FULL_REDUCTION'
  form.scope = r.scope || 'ALL'
  form.threshold = r.threshold ?? null
  form.discount = r.discount ?? null
  form.maxIssue = r.maxIssue ?? 100
  form.perLimit = r.perLimit ?? 1
  form.validRange = r.validStart && r.validEnd ? [dayjs(r.validStart), dayjs(r.validEnd)] : [dayjs(), dayjs().add(30, 'day')]
  modalOpen.value = true
}

async function handleSave() {
  if (!form.couponName || form.threshold == null || form.discount == null || form.validRange.length !== 2) {
    message.warning('请填写优惠券名称、门槛、减免和有效期')
    return
  }
  saving.value = true
  try {
    const payload = { ...form, validStart: form.validRange[0].format('YYYY-MM-DDTHH:mm:ss'), validEnd: form.validRange[1].format('YYYY-MM-DDTHH:mm:ss') }
    delete payload.validRange
    if (editingId.value) {
      await updateCoupon(editingId.value, payload)
      message.success('已更新')
    } else {
      await createCoupon(payload)
      message.success('已创建')
    }
    modalOpen.value = false
    fetchData()
  } catch { /* ignore */ } finally { saving.value = false }
}

async function handleSubmitAudit(id) { await submitAudit(id); message.success('已提交审核'); fetchData() }
async function handleAudit(id, status) { await auditCoupon(id, status); message.success(status === 2 ? '已通过' : '已驳回'); fetchData() }
async function handleDelete(id) { await deleteCoupon(id); message.success('已删除'); fetchData() }

onMounted(() => { fetchData() })
</script>
