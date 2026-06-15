<template>
  <div class="finance-page">
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
          <template v-if="column.key === 'status'">
            <a-tag v-if="record.status === 0" color="orange">待确认</a-tag>
            <a-tag v-else color="green">已确认</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space size="small">
              <a-button type="link" size="small" @click="exportExcel(record.id)">导出</a-button>
              <a-button v-if="record.status === 0" type="link" size="small" @click="confirm(record.id)">
                确认
              </a-button>
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
import { getStatementPage, confirmStatement, exportStatement } from '@/api/finance'

const loading = ref(false)
const dataSource = ref([])

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: t => `共 ${t} 条`
})

const columns = [
  { title: '对账单号', dataIndex: 'statementNo', key: 'statementNo', width: 180 },
  { title: '周期开始', dataIndex: 'periodStart', key: 'periodStart', width: 110 },
  { title: '周期结束', dataIndex: 'periodEnd', key: 'periodEnd', width: 110 },
  { title: '交易总额', dataIndex: 'totalAmount', key: 'totalAmount', width: 100 },
  { title: '关税', dataIndex: 'tariffAmount', key: 'tariffAmount', width: 90 },
  { title: '运费', dataIndex: 'shippingFee', key: 'shippingFee', width: 90 },
  { title: '退款', dataIndex: 'refundAmount', key: 'refundAmount', width: 90 },
  { title: '订单数', dataIndex: 'orderCount', key: 'orderCount', width: 80 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 140 }
]

async function fetchData() {
  loading.value = true
  try {
    const res = await getStatementPage({
      page: pagination.current,
      size: pagination.pageSize
    })
    const page = res.data
    dataSource.value = page.records || []
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

async function confirm(id) {
  try {
    await confirmStatement(id)
    message.success('已确认')
    fetchData()
  } catch { /* ignore */ }
}

async function exportExcel(id) {
  const token = localStorage.getItem('token')
  window.open(`/api/finance/statement/${id}/export?token=${token}`, '_blank')
  message.success('正在下载')
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.finance-page { max-width: 1400px; }
</style>
