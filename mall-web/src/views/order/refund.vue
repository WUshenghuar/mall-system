<template>
  <div class="sub-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">退款处理</h2>
        <p class="page-desc">审批与处理客户退款申请</p>
      </div>
    </div>

    <a-card :bordered="false">
      <a-tabs v-model:activeKey="statusFilter" @change="fetchData">
        <a-tab-pane key="" tab="全部" />
        <a-tab-pane key="0" tab="待审批" />
        <a-tab-pane key="1" tab="已通过" />
        <a-tab-pane key="2" tab="已驳回" />
      </a-tabs>
      <a-table :columns="columns" :data-source="list" :loading="loading"
        row-key="id" size="middle" :pagination="pagination" @change="onPage">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag v-if="record.refundStatus === 0" color="orange">待审批</a-tag>
            <a-tag v-else-if="record.refundStatus === 1" color="green">已通过</a-tag>
            <a-tag v-else color="default">已驳回</a-tag>
          </template>
          <template v-if="column.key === 'amount'">${{ record.refundAmount }}</template>
          <template v-if="column.key === 'comment'">
            <span v-if="record.refundStatus === 2 && record.rejectReason" class="reject-reason">
              {{ record.rejectReason }}
            </span>
            <span v-else>--</span>
          </template>
          <template v-if="column.key === 'action'">
            <a-space v-if="record.refundStatus === 0">
              <a-button size="small" type="primary" @click="handleApprove(record.id)">通过</a-button>
              <a-button size="small" danger @click="openReject(record.id)">驳回</a-button>
            </a-space>
            <span v-else>--</span>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- Reject Reason Modal -->
    <a-modal v-model:open="rejectOpen" title="驳回退款申请" @ok="handleRejectConfirm"
      :confirm-loading="rejecting" ok-text="确认驳回" ok-danger>
      <a-form layout="vertical" style="margin-top:12px">
        <a-form-item label="驳回理由" required>
          <a-textarea v-model:value="rejectReason" :rows="3"
            placeholder="请输入驳回原因，如：该订单不符合退款条件" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getRefundPage, approveRefund, rejectRefund } from '@/api/order'

const loading = ref(false)
const list = ref([])
const statusFilter = ref('')
const pagination = ref({ current: 1, pageSize: 10, total: 0 })

const rejectOpen = ref(false)
const rejecting = ref(false)
const rejectId = ref(null)
const rejectReason = ref('')

const columns = [
  { title: '订单号', dataIndex: 'orderNo' },
  { title: '退款金额', key: 'amount', width: 120 },
  { title: '原因', dataIndex: 'refundReason', ellipsis: true },
  { title: '驳回理由', key: 'comment', width: 180, ellipsis: true },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 150 }
]

async function fetchData() {
  loading.value = true
  try {
    const res = await getRefundPage({
      page: pagination.value.current,
      size: pagination.value.pageSize,
      refundStatus: statusFilter.value || undefined
    })
    list.value = res.data?.records || []
    pagination.value.total = res.data?.total || 0
  } finally { loading.value = false }
}

function onPage(p) { pagination.value.current = p.current; fetchData() }

async function handleApprove(id) {
  await approveRefund(id, '审核通过')
  message.success('已通过')
  fetchData()
}

function openReject(id) {
  rejectId.value = id
  rejectReason.value = ''
  rejectOpen.value = true
}

async function handleRejectConfirm() {
  if (!rejectReason.value.trim()) {
    message.warning('请填写驳回理由')
    return
  }
  rejecting.value = true
  try {
    await rejectRefund(rejectId.value, rejectReason.value.trim())
    message.success('已驳回')
    rejectOpen.value = false
    fetchData()
  } catch { /* ignore */ } finally { rejecting.value = false }
}

onMounted(fetchData)
</script>

<style scoped>
.reject-reason {
  color: var(--color-danger);
  font-size: var(--text-xs);
}
</style>
