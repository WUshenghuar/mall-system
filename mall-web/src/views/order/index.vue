<template>
  <div class="sub-page">
    <!-- Page head -->
    <div class="page-head">
      <div>
        <h2 class="page-title">订单管理</h2>
        <p class="page-desc">查看与处理跨境订单</p>
      </div>
      <div class="head-actions">
        <a-input-search
          v-model:value="keyword"
          placeholder="搜索订单号"
          style="width: 240px"
          @search="fetchData"
          allow-clear
        />
      </div>
    </div>

    <a-card :bordered="false">
      <a-tabs v-model:activeKey="statusFilter" @change="onTabChange">
        <a-tab-pane key="" tab="全部" />
        <a-tab-pane key="0" tab="待支付" />
        <a-tab-pane key="1" tab="待发货" />
        <a-tab-pane key="2" tab="待收货" />
        <a-tab-pane key="3" tab="已完成" />
        <a-tab-pane key="4" tab="已取消" />
      </a-tabs>

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
          <template v-if="column.key === 'orderStatus'">
            <a-tag v-if="record.orderStatus === 0">待支付</a-tag>
            <a-tag v-else-if="record.orderStatus === 1" color="blue">待发货</a-tag>
            <a-tag v-else-if="record.orderStatus === 2" color="cyan">待收货</a-tag>
            <a-tag v-else-if="record.orderStatus === 3" color="green">已完成</a-tag>
            <a-tag v-else color="default">已取消</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space size="small">
              <a-button type="link" size="small" @click="openDetail(record)">
                详情
              </a-button>
              <a-button v-if="record.orderStatus === 1" type="link" size="small" @click="openShip(record)">发货</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
      <a-modal v-model:open="detailVisible" title="交易订单详情" :footer="null"><a-descriptions v-if="selectedOrder" :column="1" bordered size="small"><a-descriptions-item label="订单号">{{ selectedOrder.orderNo }}</a-descriptions-item><a-descriptions-item label="收货人">{{ selectedOrder.receiverName }} · {{ selectedOrder.receiverPhone }}</a-descriptions-item><a-descriptions-item label="收货地址">{{ selectedOrder.receiverAddress }}</a-descriptions-item><a-descriptions-item label="应付金额">{{ selectedOrder.payAmount }}</a-descriptions-item><a-descriptions-item label="订单备注">{{ selectedOrder.remark || '无' }}</a-descriptions-item></a-descriptions></a-modal>
      <a-modal v-model:open="shipVisible" title="订单发货" @ok="submitShip"><a-form layout="vertical"><a-form-item label="物流公司" required><a-input v-model:value="shipForm.logisticsCompany" placeholder="例如：顺丰速运"/></a-form-item><a-form-item label="物流单号" required><a-input v-model:value="shipForm.logisticsNo" placeholder="请输入物流单号"/></a-form-item></a-form></a-modal>
    </a-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getTradeOrderPage, shipTradeOrder } from '@/api/order'

const loading = ref(false)
const keyword = ref('')
const statusFilter = ref('')
const dataSource = ref([])
const detailVisible = ref(false), shipVisible = ref(false), selectedOrder = ref(null)
const shipForm = reactive({ logisticsCompany: '', logisticsNo: '' })

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: t => `共 ${t} 条`
})

const columns = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 200 },
  { title: '收货人', dataIndex: 'receiverName', key: 'receiverName', width: 130 },
  { title: '应付金额', dataIndex: 'payAmount', key: 'payAmount', width: 120 },
  { title: '状态', key: 'orderStatus', width: 90 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 160 }
]

async function fetchData() {
  loading.value = true
  try {
    const res = await getTradeOrderPage({
      page: pagination.current,
      size: pagination.pageSize,
      orderStatus: statusFilter.value || undefined,
      keyword: keyword.value
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

function onTabChange() {
  pagination.current = 1
  fetchData()
}

function openDetail(record) { selectedOrder.value = record; detailVisible.value = true }
function openShip(record) { selectedOrder.value = record; shipForm.logisticsCompany = ''; shipForm.logisticsNo = ''; shipVisible.value = true }
async function submitShip() { if (!shipForm.logisticsCompany || !shipForm.logisticsNo) return message.warning('请填写物流公司和物流单号'); try { await shipTradeOrder(selectedOrder.value.orderNo, shipForm); message.success('发货成功'); shipVisible.value = false; fetchData() } catch { /* handled by interceptor */ } }

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.order-page { max-width: 1400px; }
</style>
