<template>
  <section class="page orders-page">
    <h2>我的货运单</h2>
    <van-loading v-if="loading" />
    <div v-for="order in orders" :key="order.orderNo" class="card order-card">
      <b class="order-number">订单号：{{ order.orderNo }}</b>
      <p class="money">{{ order.payAmount }}</p>
      <p class="note">{{ label(order.orderStatus) }} · {{ order.createTime }}</p>
      <div class="order-actions">
        <van-button plain @click="detail(order.orderNo)">订单详情</van-button>
        <van-button v-if="order.orderStatus === 0" plain type="danger" @click="cancel(order.orderNo)">取消订单</van-button>
        <van-button v-if="order.orderStatus === 2" type="primary" @click="confirm(order.orderNo)">确认收货</van-button>
        <van-button v-if="order.orderStatus === 2" plain @click="track(order.orderNo)">查看物流</van-button>
      </div>
    </div>
    <van-empty v-if="!loading && !orders.length" description="还没有订单" />
  </section>
</template>
<script setup>
import{ref,onMounted,onUnmounted}from'vue';import{showToast}from'vant';import{tradeApi}from'../api';import{useRouter}from'vue-router'
const orders=ref([]),loading=ref(false),router=useRouter();const labels=['待支付','待发货','待收货','已完成','已取消','退款处理中','已退款'];const label=s=>labels[s]||'处理中';let refreshTimer;async function load(){loading.value=true;try{orders.value=(await tradeApi.orders()).data.records||[]}catch(e){showToast(e)}finally{loading.value=false}}async function confirm(orderNo){try{await tradeApi.confirmOrder(orderNo);load()}catch(e){showToast(e)}}async function cancel(orderNo){try{await tradeApi.cancelOrder(orderNo);load()}catch(e){showToast(e)}}function detail(orderNo){router.push(`/orders/${orderNo}`)}function track(orderNo){detail(orderNo)}onMounted(()=>{load();refreshTimer=window.setInterval(load,30000)});onUnmounted(()=>window.clearInterval(refreshTimer))
</script>
