<template><section class="page"><h2>确认收货与付款</h2><van-cell-group inset><van-field v-model="addressId" label="收货地址 ID" placeholder="请先在“我的”添加地址" type="digit"/><van-field v-model="remark" label="订单备注" placeholder="选填"/></van-cell-group><div class="card"><p>已选 {{ selected.length }} 件商品</p><p class="money">应付 {{ settlement?.payAmount ?? '--' }}</p><van-button block type="primary" :disabled="!addressId||!selected.length" @click="submit">创建订单并模拟支付</van-button></div></section></template>
<script setup>
import{ref,onMounted,watch}from'vue';import{showToast}from'vant';import{tradeApi}from'../api';import{useRouter}from'vue-router'
const carts=ref([]),settlement=ref(null),addressId=ref(''),remark=ref(''),router=useRouter();const selected=ref([])
async function load(){try{carts.value=(await tradeApi.cart()).data||[];selected.value=carts.value.filter(x=>x.checked===1)}catch(e){showToast(e)}}
async function preview(){if(!addressId.value||!selected.value.length)return;try{settlement.value=(await tradeApi.settle({cartIds:selected.value.map(x=>x.id),addressId:+addressId.value})).data}catch(e){showToast(e)}}
async function submit(){try{await preview();const items=selected.value.map(x=>({skuId:x.skuId,quantity:x.quantity}));const order=(await tradeApi.createOrder({addressId:+addressId.value,remark:remark.value,items:JSON.stringify(items)})).data;const pay=(await tradeApi.pay({orderNo:order.orderNo,payType:1})).data;await tradeApi.simulate(pay.payNo);showToast('支付成功，等待发货');router.replace('/orders')}catch(e){showToast(e)}}
watch(addressId,preview);onMounted(load)
</script>
