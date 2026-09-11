<template>
  <section class="checkout-page">
    <div class="checkout-heading"><p class="eyebrow">CHECKOUT</p><h1>确认收货与付款</h1><p>选择收货地址后，我们会再次核对价格与库存。</p></div>
    <section class="checkout-addresses"><div class="section-heading"><h2>选择收货地址</h2><van-button size="small" plain type="primary" @click="router.push('/account')">管理地址</van-button></div>
      <van-radio-group v-if="addresses.length" v-model="addressId"><div v-for="address in addresses" :key="address.id" class="checkout-address" :class="{ selected: addressId === address.id }" @click="addressId = address.id"><van-radio :name="address.id" checked-color="#0c5d75"/><div><b>{{ address.receiverName }} · {{ address.receiverPhone }}</b><van-tag v-if="address.isDefault === 1" type="success" plain>默认</van-tag><p>{{ address.province }}{{ address.city }}{{ address.district }}{{ address.detailAddress }}</p></div></div></van-radio-group>
      <van-empty v-else image="search" description="请先添加收货地址"><van-button type="primary" @click="router.push('/account')">去添加地址</van-button></van-empty>
    </section>
    <section v-if="settlement?.items?.length" class="checkout-items">
      <div v-for="item in settlement.items" :key="item.cartId" class="checkout-item">
        <span>{{ item.skuCode }} × {{ item.quantity }}</span>
        <span><del v-if="hasActivityPrice(item)">{{ item.originalPrice }}</del> {{ item.subtotal }}</span>
      </div>
    </section>
    <section v-if="settlement?.availableCoupons?.length" class="checkout-coupons">
      <div class="section-heading"><h2>可用优惠券</h2><span class="note">仅展示当前结算可用</span></div>
      <van-radio-group v-model="couponId">
        <van-radio v-for="coupon in settlement.availableCoupons" :key="coupon.issueId || coupon.couponId" :name="coupon.couponId" class="card checkout-coupon" checked-color="#0c5d75">
          <div><strong>{{ couponText(coupon) }}</strong><p>{{ coupon.couponName }} · 满 {{ coupon.threshold || 0 }} 可用</p></div>
        </van-radio>
      </van-radio-group>
      <van-button v-if="couponId" plain type="primary" @click="couponId = null">不使用优惠券</van-button>
    </section>
    <van-cell-group inset><van-field v-model="remark" label="订单备注" placeholder="选填"/></van-cell-group>
    <div class="checkout-summary"><p>已选 {{ selected.length }} 件商品</p><p v-if="settlement?.discountAmount > 0">优惠 -{{ settlement.discountAmount }}</p><p class="money">应付 {{ settlement?.payAmount ?? '--' }}</p><van-button block type="primary" :loading="submitting" :disabled="!addressId || !selected.length" @click="submit">创建订单并模拟支付</van-button></div>
  </section>
</template>
<script setup>
import { ref, onMounted, watch } from 'vue'
import { showToast } from 'vant'
import { memberApi, tradeApi } from '../api'
import { useRouter } from 'vue-router'
const carts = ref([]), settlement = ref(null), addresses = ref([]), addressId = ref(null), couponId = ref(null), remark = ref(''), selected = ref([]), submitting = ref(false), router = useRouter()
const couponText = coupon => coupon.couponType === 'DISCOUNT' ? `${coupon.discount} 折` : coupon.couponType === 'SHIPPING' ? '包邮' : `减 ¥${coupon.discount}`
const hasActivityPrice = item => item.originalPrice != null && item.price != null && Number(item.originalPrice) > Number(item.price)
async function load() { try { const [cartRes, addressRes] = await Promise.all([tradeApi.cart(), memberApi.addresses()]); carts.value = cartRes.data || []; selected.value = carts.value.filter(x => x.checked === 1); addresses.value = addressRes.data || []; addressId.value = addresses.value.find(x => x.isDefault === 1)?.id || addresses.value[0]?.id || null; await preview() } catch (e) { showToast(e) } }
async function preview() { if (!addressId.value || !selected.value.length) { settlement.value = null; return false } try { settlement.value = (await tradeApi.settle({ cartIds: selected.value.map(x => x.id), addressId: addressId.value, couponId: couponId.value })).data; return true } catch (e) { settlement.value = null; showToast(e); return false } }
async function submit() { if (!(await preview()) || !settlement.value?.snapshotToken) return; submitting.value = true; try { const order = (await tradeApi.createOrder({ snapshotToken: settlement.value.snapshotToken, remark: remark.value })).data; const pay = (await tradeApi.pay({ orderNo: order.orderNo, payType: 1 })).data; await tradeApi.simulate(pay.payNo); showToast('支付成功，等待发货'); router.replace('/orders') } catch (e) { showToast(e) } finally { submitting.value = false } }
watch([addressId, couponId], preview); onMounted(load)
</script>
<style scoped>
.checkout-items{margin:16px 0;padding:12px 14px;border:1px solid var(--line);border-radius:12px;background:#fbfdfd}.checkout-item{display:flex;justify-content:space-between;gap:12px;padding:6px 0;color:#64748b;font-size:13px}.checkout-item span:last-child{color:var(--ink);font-weight:700}.checkout-item del{margin-right:5px;color:#9aa5ae;font-weight:400}
.checkout-coupon{display:flex;align-items:center;gap:12px;min-height:72px;cursor:pointer}.checkout-coupon>div{min-width:0;flex:1}.checkout-coupon strong{display:block;color:var(--copper);font-size:21px}.checkout-coupon p{margin:4px 0 0;color:#64748b;font-size:13px}.checkout-coupon :deep(.van-radio__label){flex:1}.checkout-coupons>.van-button{margin-bottom:12px;min-height:44px}
</style>
