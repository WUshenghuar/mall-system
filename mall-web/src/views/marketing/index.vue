<template>
  <div class="marketing-page">
    <a-card :bordered="false">
      <template #extra>
        <a-button type="primary" @click="showAddModal = true">
          <plus-outlined /> 新建优惠券
        </a-button>
      </template>

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
          <template v-if="column.key === 'status'">
            <a-tag v-if="record.status === 0">草稿</a-tag>
            <a-tag v-else-if="record.status === 1" color="orange">待审核</a-tag>
            <a-tag v-else-if="record.status === 2" color="green">已发布</a-tag>
            <a-tag v-else color="default">已结束</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space size="small">
              <a-button v-if="record.status === 0" type="link" size="small" @click="handleSubmitAudit(record.id)">
                提交审核
              </a-button>
              <a-button v-if="record.status === 1" type="link" size="small" @click="handleAudit(record.id, 2)">
                审核通过
              </a-button>
              <a-button v-if="record.status === 1" type="link" size="small" danger @click="handleAudit(record.id, 3)">
                驳回
              </a-button>
              <a-popconfirm title="确认删除？" @confirm="handleDelete(record.id)">
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { getCouponPage, deleteCoupon, submitAudit, auditCoupon } from '@/api/marketing'

const loading = ref(false)
const dataSource = ref([])
const showAddModal = ref(false)

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: t => `共 ${t} 条`
})

const columns = [
  { title: '优惠券名称', dataIndex: 'couponName', key: 'couponName' },
  { title: '类型', dataIndex: 'couponType', key: 'couponType', width: 120 },
  { title: '门槛', dataIndex: 'threshold', key: 'threshold', width: 80 },
  { title: '减免', dataIndex: 'discount', key: 'discount', width: 80 },
  { title: '发行/总量', key: 'issued', width: 90 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 240 }
]

async function fetchData() {
  loading.value = true
  try {
    const res = await getCouponPage({
      page: pagination.current,
      size: pagination.pageSize
    })
    const page = res.data
    // add computed issued field
    dataSource.value = (page.records || []).map(r => ({
      ...r,
      issued: `${r.issuedCount || 0}/${r.maxIssue || 0}`
    }))
    pagination.total = page.total || 0
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
}

function onTableChange(pag) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

async function handleSubmitAudit(id) {
  try {
    await submitAudit(id)
    message.success('已提交审核')
    fetchData()
  } catch { /* ignore */ }
}

async function handleAudit(id, status) {
  try {
    await auditCoupon(id, status)
    message.success(status === 2 ? '审核通过' : '已驳回')
    fetchData()
  } catch { /* ignore */ }
}

async function handleDelete(id) {
  try {
    await deleteCoupon(id)
    message.success('已删除')
    fetchData()
  } catch { /* ignore */ }
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.marketing-page { max-width: 1200px; }
</style>
