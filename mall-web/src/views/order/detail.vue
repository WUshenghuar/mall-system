<template>
  <div class="sub-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">订单详情</h2>
        <p class="page-desc">{{ order?.orderNo || '加载中…' }}</p>
      </div>
      <div class="head-actions">
        <a-space>
          <a-button @click="$router.back()">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="margin-right:4px;vertical-align:middle"><polyline points="15 18 9 12 15 6"/></svg>
            返回
          </a-button>
          <a-button v-if="order?.orderStatus === 0" type="primary" @click="handlePay">模拟支付</a-button>
          <a-button v-if="order && order.orderStatus !== 4 && order.orderStatus !== 5" @click="$router.push('/order/refund')">
            查看退款
          </a-button>
        </a-space>
      </div>
    </div>

    <a-card :bordered="false" :loading="loading">

      <a-descriptions v-if="order" bordered :column="2" size="small" style="margin-bottom:24px">
        <a-descriptions-item label="订单号">{{ order.orderNo }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag v-if="order.orderStatus === 0">待支付</a-tag>
          <a-tag v-else-if="order.orderStatus === 1" color="blue">已支付</a-tag>
          <a-tag v-else-if="order.orderStatus === 2" color="cyan">已发货</a-tag>
          <a-tag v-else-if="order.orderStatus === 3" color="green">已签收</a-tag>
          <a-tag v-else color="default">已取消</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="商品总金额">${{ order.totalAmount }}</a-descriptions-item>
        <a-descriptions-item label="币种">{{ order.currency }}</a-descriptions-item>
        <a-descriptions-item label="关税">${{ order.tariffAmount || '0.00' }}</a-descriptions-item>
        <a-descriptions-item label="运费">${{ order.shippingFee || '0.00' }}</a-descriptions-item>
        <a-descriptions-item label="实付金额" :span="2">
          <strong style="font-size:16px;color:var(--color-brass)">${{ order.payAmount }}</strong>
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ order.createTime }}</a-descriptions-item>
        <a-descriptions-item label="支付时间">{{ order.payTime || '--' }}</a-descriptions-item>
      </a-descriptions>

      <!-- Order Items -->
      <h3 v-if="order" class="section-title">商品明细</h3>
      <a-table v-if="order" :columns="itemCols" :data-source="orderItems"
        :loading="itemsLoading" row-key="id" size="middle" :pagination="false" style="margin-bottom:24px">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'price'">${{ record.price }}</template>
          <template v-if="column.key === 'subtotal'">${{ (record.price * record.quantity).toFixed(2) }}</template>
        </template>
      </a-table>
      <a-empty v-if="!loading && order && orderItems.length === 0" description="暂无商品明细" />

      <!-- Logistics -->
      <h3 v-if="order && order.orderStatus >= 2" class="section-title">物流信息</h3>
      <a-descriptions v-if="order && order.orderStatus >= 2" bordered :column="2" size="small">
        <a-descriptions-item label="物流单号">{{ order.logisticsNo || '--' }}</a-descriptions-item>
        <a-descriptions-item label="物流公司">{{ order.logisticsCompany || '--' }}</a-descriptions-item>
        <a-descriptions-item label="发货时间">{{ order.shipTime || '--' }}</a-descriptions-item>
        <a-descriptions-item label="预计到达">{{ order.estimatedArrival || '--' }}</a-descriptions-item>
      </a-descriptions>
    </a-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { getOrderDetail, payOrder } from '@/api/order'
import request from '@/utils/request'

const route = useRoute()
const order = ref(null)
const loading = ref(false)
const orderItems = ref([])
const itemsLoading = ref(false)

const itemCols = [
  { title: '商品名称', dataIndex: 'spuName', ellipsis: true },
  { title: 'SKU 编码', dataIndex: 'skuCode' },
  { title: '数量', dataIndex: 'quantity', width: 80 },
  { title: '单价', key: 'price', width: 100 },
  { title: '小计', key: 'subtotal', width: 120 }
]

async function fetchData() {
  loading.value = true
  try {
    const res = await getOrderDetail(route.params.id)
    order.value = res.data
    // fetch order items
    if (res.data?.id) {
      itemsLoading.value = true
      try {
        const itemsRes = await request.get(`/order/${res.data.id}/items`)
        orderItems.value = itemsRes.data || []
      } catch { orderItems.value = [] } finally { itemsLoading.value = false }
    }
  } finally { loading.value = false }
}

async function handlePay() {
  try {
    await payOrder(route.params.id)
    message.success('支付成功')
    fetchData()
  } catch { /* ignore */ }
}

onMounted(fetchData)
</script>

<style scoped>
.section-title {
  font-family: var(--font-sans);
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--color-lead);
  margin-bottom: var(--space-3);
}
</style>
