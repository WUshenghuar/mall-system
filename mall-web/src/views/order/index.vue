<template>
  <div class="order-page">
    <a-card :bordered="false">
      <template #extra>
        <a-input-search
          v-model:value="keyword"
          placeholder="搜索订单号"
          style="width: 220px"
          @search="fetchData"
          allow-clear
        />
      </template>

      <a-tabs v-model:activeKey="statusFilter" @change="onTabChange">
        <a-tab-pane key="" tab="全部" />
        <a-tab-pane key="0" tab="待支付" />
        <a-tab-pane key="1" tab="已支付" />
        <a-tab-pane key="2" tab="已发货" />
        <a-tab-pane key="3" tab="已签收" />
        <a-tab-pane key="5" tab="已取消" />
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
            <a-tag v-else-if="record.orderStatus === 1" color="blue">已支付</a-tag>
            <a-tag v-else-if="record.orderStatus === 2" color="cyan">已发货</a-tag>
            <a-tag v-else-if="record.orderStatus === 3" color="green">已签收</a-tag>
            <a-tag v-else-if="record.orderStatus === 4" color="green">已完成</a-tag>
            <a-tag v-else color="default">已取消</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space size="small">
              <a-button type="link" size="small" @click="$router.push(`/order/${record.id}`)">
                详情
              </a-button>
              <a-button
                v-if="record.orderStatus === 0"
                type="link"
                size="small"
                @click="handlePay(record)"
              >支付</a-button>
              <a-button
                v-if="record.orderStatus === 0"
                type="link"
                size="small"
                danger
                @click="handleCancel(record)"
              >取消</a-button>
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
import { getOrderPage, payOrder, cancelOrder, getRefundPage } from '@/api/order'

const loading = ref(false)
const keyword = ref('')
const statusFilter = ref('')
const dataSource = ref([])

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: t => `共 ${t} 条`
})

const columns = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 200 },
  { title: '金额(USD)', dataIndex: 'payAmount', key: 'payAmount', width: 120 },
  { title: '币种', dataIndex: 'currency', key: 'currency', width: 70 },
  { title: '关税', dataIndex: 'tariffAmount', key: 'tariffAmount', width: 100 },
  { title: '运费', dataIndex: 'shippingFee', key: 'shippingFee', width: 100 },
  { title: '状态', key: 'orderStatus', width: 90 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 160 }
]

async function fetchData() {
  loading.value = true
  try {
    const res = await getOrderPage({
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

async function handlePay(record) {
  try {
    await payOrder(record.id)
    message.success('支付成功')
    fetchData()
  } catch {
    // ignore
  }
}

async function handleCancel(record) {
  try {
    await cancelOrder(record.id)
    message.success('已取消')
    fetchData()
  } catch {
    // ignore
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.order-page {
  max-width: 1400px;
}
</style>
