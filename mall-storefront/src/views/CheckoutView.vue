<template>
  <section class="checkout-page">
    <div class="checkout-heading"><p class="eyebrow">CHECKOUT</p><h1>确认收货与付款</h1><p>选择收货地址后，我们会再次核对价格与库存。</p></div>
    <section class="checkout-addresses"><div class="section-heading"><h2>选择收货地址</h2><van-button size="small" plain type="primary" @click="router.push('/account')">管理地址</van-button></div>
      <van-radio-group v-if="addresses.length" v-model="addressId"><div v-for="address in addresses" :key="address.id" class="checkout-address" :class="{ selected: addressId === address.id }" @click="addressId = address.id"><van-radio :name="address.id" checked-color="#0c5d75"/><div><b>{{ address.receiverName }} · {{ address.receiverPhone }}</b><van-tag v-if="address.isDefault === 1" type="success" plain>默认</van-tag><p>{{ address.province }}{{ address.city }}{{ address.district }}{{ address.detailAddress }}</p></div></div></van-radio-group>
      <van-empty v-else image="search" description="请先添加收货地址"><van-button type="primary" @click="router.push('/account')">去添加地址</van-button></van-empty>
    </section>
    <van-cell-group inset><van-field v-model="remark" label="订单备注" placeholder="选填"/></van-cell-group>
    <div class="checkout-summary"><p>已选 {{ selected.length }} 件商品</p><p class="money">应付 {{ settlement?.payAmount ?? '--' }}</p><van-button block type="primary" :loading="submitting" :disabled="!addressId || !selected.length" @click="submit">创建订单并模拟支付</van-button></div>
  </section>
</template>
<script setup>
import { ref, onMounted, watch } from 'vue'
import { showToast } from 'vant'
import { memberApi, tradeApi } from '../api'
import { useRouter } from 'vue-router'
const carts = ref([]), settlement = ref(null), addresses = ref([]), addressId = ref(null), remark = ref(''), selected = ref([]), submitting = ref(false), router = useRouter()
async function load() { try { const [cartRes, addressRes] = await Promise.all([tradeApi.cart(), memberApi.addresses()]); carts.value = cartRes.data || []; selected.value = carts.value.filter(x => x.checked === 1); addresses.value = addressRes.data || []; addressId.value = addresses.value.find(x => x.isDefault === 1)?.id || addresses.value[0]?.id || null; await preview() } catch (e) { showToast(e) } }
async function preview() { if (!addressId.value || !selected.value.length) { settlement.value = null; return false } try { settlement.value = (await tradeApi.settle({ cartIds: selected.value.map(x => x.id), addressId: addressId.value })).data; return true } catch (e) { settlement.value = null; showToast(e); return false } }
async function submit() { if (!(await preview()) || !settlement.value?.snapshotToken) return; submitting.value = true; try { const order = (await tradeApi.createOrder({ snapshotToken: settlement.value.snapshotToken, remark: remark.value })).data; const pay = (await tradeApi.pay({ orderNo: order.orderNo, payType: 1 })).data; await tradeApi.simulate(pay.payNo); showToast('支付成功，等待发货'); router.replace('/orders') } catch (e) { showToast(e) } finally { submitting.value = false } }
watch(addressId, preview); onMounted(load)
</script>
