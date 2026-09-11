<template>
  <div class="sub-page">
    <!-- Page head -->
    <div class="page-head">
      <div>
        <h2 class="page-title">对账单</h2>
        <p class="page-desc">周期对账、确认与导出</p>
      </div>
      <a-button type="primary" @click="generate">生成本月对账单</a-button>
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
          <template v-if="column.key === 'status'">
            <a-tag v-if="record.status === 0" color="orange">待确认</a-tag>
            <a-tag v-else color="green">已确认</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space size="small">
              <a-button type="link" size="small" @click="viewDetail(record.id)">明细</a-button>
              <a-button type="link" size="small" @click="exportExcel(record.id)">导出</a-button>
              <a-button v-if="record.status === 0" type="link" size="small" @click="confirm(record.id)">
                确认
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-drawer v-model:open="detailOpen" title="对账单明细" :width="760">
      <a-spin :spinning="detailLoading">
        <template v-if="detail">
          <a-descriptions bordered :column="2" size="small">
            <a-descriptions-item label="对账单号">{{ detail.statement?.statementNo }}</a-descriptions-item>
            <a-descriptions-item label="周期">
              {{ detail.statement?.periodStart }} ~ {{ detail.statement?.periodEnd }}
            </a-descriptions-item>
            <a-descriptions-item label="交易总额">{{ detail.statement?.totalAmount }}</a-descriptions-item>
            <a-descriptions-item label="税费">{{ detail.statement?.tariffAmount }}</a-descriptions-item>
            <a-descriptions-item label="运费">{{ detail.statement?.shippingFee }}</a-descriptions-item>
            <a-descriptions-item label="退款">{{ detail.statement?.refundAmount }}</a-descriptions-item>
            <a-descriptions-item label="净额">{{ detail.statement?.netAmount }}</a-descriptions-item>
            <a-descriptions-item label="订单数">{{ detail.statement?.orderCount }}</a-descriptions-item>
          </a-descriptions>

          <a-table
            :columns="detailColumns"
            :data-source="detail.items || []"
            :pagination="false"
            :scroll="{ x: 680 }"
            row-key="id"
            size="small"
            style="margin-top: 16px"
          />
        </template>
      </a-spin>
      <a-empty v-if="!detailLoading && !detail" description="暂无对账单明细" />
    </a-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getStatementPage, getStatementDetail, confirmStatement, exportStatement, generateStatement } from '@/api/finance'

const loading = ref(false)
const dataSource = ref([])
const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

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

const detailColumns = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 170 },
  { title: '交易金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 100 },
  { title: '税费', dataIndex: 'tariffAmount', key: 'tariffAmount', width: 90 },
  { title: '运费', dataIndex: 'shippingFee', key: 'shippingFee', width: 90 },
  { title: '退款', dataIndex: 'refundAmount', key: 'refundAmount', width: 90 },
  { title: '实付', dataIndex: 'payAmount', key: 'payAmount', width: 100 },
  { title: '下单时间', dataIndex: 'orderTime', key: 'orderTime', width: 160 }
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

async function viewDetail(id) {
  detailOpen.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = (await getStatementDetail(id)).data
  } catch { /* handled by interceptor */
  } finally {
    detailLoading.value = false
  }
}

async function confirm(id) {
  try {
    await confirmStatement(id)
    message.success('已确认')
    fetchData()
  } catch { /* ignore */ }
}

async function generate() {
  try {
    await generateStatement()
    message.success('本月对账单已生成')
    fetchData()
  } catch { /* handled by interceptor */ }
}

async function exportExcel(id) {
  try {
    const blob = await exportStatement(id)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `对账单-${id}.xlsx`
    link.click()
    URL.revokeObjectURL(url)
    message.success('对账单已下载')
  } catch { /* handled by interceptor */ }
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.finance-page { max-width: 1400px; }
</style>
