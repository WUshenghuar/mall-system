<template>
  <section class="page order-detail-page">
    <van-nav-bar title="订单详情" left-text="返回" left-arrow @click-left="router.back" />
    <van-loading v-if="loading" class="page-loading" />
    <template v-else-if="order">
      <section class="card detail-card">
        <p class="eyebrow">ORDER</p><h2>{{ statusLabel }}</h2>
        <p class="note">订单号：{{ order.orderNo }}</p>
        <div class="amount-row"><span>实付金额</span><strong>{{ order.currency || '' }} {{ order.payAmount }}</strong></div>
        <p class="note">创建于 {{ order.createTime }}</p>
      </section>
      <section class="card detail-card"><h3>收货信息</h3><p>{{ order.receiverName }} · {{ order.receiverPhone }}</p><p class="note">{{ order.receiverAddress }}</p></section>
      <section class="card detail-card"><h3>费用明细</h3><p>商品金额 <span>{{ order.totalAmount }}</span></p><p>优惠 <span>-{{ order.discountAmount }}</span></p><p>运费 <span>{{ order.freightAmount }}</span></p><p v-if="order.taxAmount > 0">税费 <span>{{ order.taxAmount }}</span></p></section>
      <section v-if="order.orderStatus >= 2" class="card detail-card"><h3>物流信息</h3><template v-if="logistics"><p>{{ logistics.logisticsCompany || '承运商待录入' }}</p><p class="note">运单号：{{ logistics.logisticsNo || '--' }}</p></template><van-empty v-else description="暂未录入物流信息" /></section>
      <div class="order-actions"><van-button v-if="order.orderStatus === 0" plain type="danger" @click="cancel">取消订单</van-button><van-button v-if="order.orderStatus === 1" plain type="warning" @click="refund(0)">申请仅退款</van-button><van-button v-if="[2, 3].includes(order.orderStatus)" plain type="warning" @click="refund(1)">申请退货退款</van-button><van-button v-if="order.orderStatus === 2" type="primary" @click="confirm">确认收货</van-button></div>
    </template>
    <van-empty v-else description="订单不存在或无权查看" />
  </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import { tradeApi } from '../api'

const route = useRoute(), router = useRouter(), order = ref(null), logistics = ref(null), loading = ref(true)
let refreshTimer
const labels = ['待支付', '待发货', '待收货', '已完成', '已取消', '退款中', '已退款']
const statusLabel = computed(() => labels[order.value?.orderStatus] || '处理中')
async function load() { loading.value = true; try { const result = await tradeApi.order(route.params.orderNo); order.value = result.data; if (order.value?.orderStatus >= 2) logistics.value = (await tradeApi.logistics(route.params.orderNo)).data } catch (e) { showToast(e) } finally { loading.value = false } }
async function cancel() { try { await showConfirmDialog({ title: '取消订单', message: '取消后已锁定库存将被释放。' }); await tradeApi.cancelOrder(order.value.orderNo); showToast('订单已取消'); load() } catch (e) { if (e !== 'cancel') showToast(e) } }
async function confirm() { try { await tradeApi.confirmOrder(order.value.orderNo); showToast('已确认收货'); load() } catch (e) { showToast(e) } }
function refund(type) { router.push({ path: '/refunds', query: { orderNo: order.value.orderNo, type } }) }
onMounted(async () => { await load(); if (order.value?.orderStatus === 5) refreshTimer = window.setInterval(load, 15000) })
onUnmounted(() => window.clearInterval(refreshTimer))
</script>

<style scoped>
.order-detail-page { padding-bottom: 88px; }.page-loading { display: block; margin: 48px auto; }.detail-card { margin-bottom: 12px; }.detail-card h3 { margin: 0 0 12px; }.detail-card p { display: flex; justify-content: space-between; gap: 12px; }.detail-card .note { display: block; line-height: 1.6; }.amount-row { display: flex; justify-content: space-between; align-items: center; margin: 18px 0; }.amount-row strong { font-size: 24px; color: var(--color-ocean, #0c5d75); }.order-actions { display: flex; gap: 12px; }.order-actions .van-button { flex: 1; min-height: 44px; }
@media (min-width: 1025px){ .order-detail-page { padding-bottom: 24px; max-width: 900px; margin: 0 auto; } }
</style>
